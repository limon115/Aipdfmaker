package com.example.domain.services.updater

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.example.utils.AppLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class GitHubArtifactService(private val context: Context) {

    companion object {
        const val REPO_OWNER = "limon115"
        const val REPO_NAME = "Aipdfmaker"
        const val ARTIFACT_NAME = "DocMorph-Debug-APK"
        const val EXPECTED_PACKAGE_NAME = "com.aistudio.docmorph.kqbzpa"
        private const val BASE_GITHUB_API = "https://api.github.com"
        private const val TAG = "GitHubArtifactService"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Queries the GitHub Actions API for the latest DocMorph-Debug-APK workflow artifact.
     */
    suspend fun checkForUpdate(token: String? = null): Result<UpdateInfo> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val url = "$BASE_GITHUB_API/repos/$REPO_OWNER/$REPO_NAME/actions/artifacts?name=$ARTIFACT_NAME&per_page=5"
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DocMorph-Android-Updater")

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            val request = requestBuilder.build()
            val bodyString = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errBody = response.body?.string() ?: ""
                    AppLogger.w(TAG, "GitHub API returned error $code: $errBody")
                    if (code == 401 || code == 403) {
                        return@withContext Result.failure(
                            IllegalStateException("GitHub API authorization error ($code). If using a token, ensure it has repository/actions read permissions.")
                        )
                    }
                    return@withContext Result.failure(
                        IllegalStateException("GitHub API error ($code): ${response.message}")
                    )
                }
                response.body?.string() ?: ""
            }

            val json = JSONObject(bodyString)
            val artifactsArray = json.optJSONArray("artifacts")

            if (artifactsArray == null || artifactsArray.length() == 0) {
                return@withContext Result.failure(
                    IllegalStateException("No artifacts named '$ARTIFACT_NAME' found in repository $REPO_OWNER/$REPO_NAME.")
                )
            }

            // Find the most recent non-expired artifact matching our target name
            var matchedArtifact: GitHubArtifact? = null
            for (i in 0 until artifactsArray.length()) {
                val item = artifactsArray.getJSONObject(i)
                val name = item.optString("name", "")
                val expired = item.optBoolean("expired", false)
                if (name == ARTIFACT_NAME && !expired) {
                    val workflowRunObj = item.optJSONObject("workflow_run")
                    matchedArtifact = GitHubArtifact(
                        id = item.getLong("id"),
                        name = name,
                        sizeInBytes = item.optLong("size_in_bytes", 0L),
                        archiveDownloadUrl = item.optString("archive_download_url", ""),
                        createdAt = item.optString("created_at", ""),
                        workflowRunId = workflowRunObj?.optLong("id") ?: 0L,
                        headSha = workflowRunObj?.optString("head_sha", "") ?: "",
                        headBranch = workflowRunObj?.optString("head_branch", "main") ?: "main"
                    )
                    break
                }
            }

            if (matchedArtifact == null) {
                return@withContext Result.failure(
                    IllegalStateException("No active '$ARTIFACT_NAME' artifacts found in repository.")
                )
            }

            // Gather local application package info
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val currentVersionName = packageInfo.versionName ?: "1.0"
            val lastUpdateTime = packageInfo.lastUpdateTime

            val artifactTimestampMs = parseIsoDate(matchedArtifact.createdAt)
            // Consider artifact newer if its build timestamp is ahead of the app's install/update time
            val isNewer = artifactTimestampMs > (lastUpdateTime + 60_000L) // 1 min buffer

            val updateInfo = UpdateInfo(
                artifact = matchedArtifact,
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                isNewer = isNewer,
                formattedSize = formatBytes(matchedArtifact.sizeInBytes),
                formattedDate = formatDate(artifactTimestampMs)
            )

            AppLogger.i(TAG, "Update check complete: artifact=${matchedArtifact.id}, size=${updateInfo.formattedSize}, isNewer=$isNewer")
            Result.success(updateInfo)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the artifact ZIP file from GitHub Actions with streaming progress and speed reporting.
     * Uses optimized 128KB buffer with buffered streams to maximize network & flash storage throughput.
     */
    suspend fun downloadArtifactZip(
        artifact: GitHubArtifact,
        token: String?,
        outputZipFile: File,
        onProgress: (downloaded: Long, total: Long, speedBytesPerSec: Long) -> Unit
    ): Result<File> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("A GitHub Personal Access Token (PAT) is required to download workflow artifacts.")
            )
        }

        try {
            val downloadUrl = if (artifact.archiveDownloadUrl.isNotBlank()) {
                artifact.archiveDownloadUrl
            } else {
                "$BASE_GITHUB_API/repos/$REPO_OWNER/$REPO_NAME/actions/artifacts/${artifact.id}/zip"
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DocMorph-Android-Updater")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val code = response.code
                if (code == 401 || code == 403) {
                    return@withContext Result.failure(
                        IllegalStateException("GitHub authentication failed ($code). Please check that your token has 'actions:read' or repository scope.")
                    )
                }
                return@withContext Result.failure(
                    IllegalStateException("Download failed ($code): ${response.message}")
                )
            }

            val responseBody = response.body ?: return@withContext Result.failure(
                IllegalStateException("Response body is empty.")
            )

            val totalBytes = if (responseBody.contentLength() > 0) {
                responseBody.contentLength()
            } else {
                artifact.sizeInBytes
            }

            outputZipFile.parentFile?.mkdirs()
            if (outputZipFile.exists()) {
                outputZipFile.delete()
            }

            // High-throughput 128KB buffer and buffered streams to maximize network & flash storage I/O
            val bufferSize = 128 * 1024
            val inputStream = BufferedInputStream(responseBody.byteStream(), bufferSize)
            val outputStream = BufferedOutputStream(FileOutputStream(outputZipFile), bufferSize)

            val buffer = ByteArray(bufferSize)
            var bytesRead: Int
            var totalRead = 0L
            val downloadStartTime = System.currentTimeMillis()
            var lastProgressEmit = 0L
            var lastSpeedSampleTime = downloadStartTime
            var lastSpeedSampleBytes = 0L
            var smoothedSpeed = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        val speedSampleElapsed = now - lastSpeedSampleTime

                        // Compute rolling smoothed download speed every ~250ms
                        if (speedSampleElapsed >= 250L) {
                            val bytesDelta = totalRead - lastSpeedSampleBytes
                            val instantSpeed = (bytesDelta * 1000L) / speedSampleElapsed
                            smoothedSpeed = if (smoothedSpeed <= 0L) {
                                instantSpeed
                            } else {
                                ((smoothedSpeed * 0.7) + (instantSpeed * 0.3)).toLong()
                            }
                            lastSpeedSampleTime = now
                            lastSpeedSampleBytes = totalRead
                        }

                        // Emit progress to UI at ~150-200ms intervals or on completion
                        if (now - lastProgressEmit >= 150L || totalRead == totalBytes) {
                            lastProgressEmit = now
                            val currentSpeed = if (smoothedSpeed > 0L) {
                                smoothedSpeed
                            } else {
                                val elapsed = now - downloadStartTime
                                if (elapsed > 50L) (totalRead * 1000L) / elapsed else 0L
                            }
                            onProgress(totalRead, totalBytes, currentSpeed)
                        }
                    }
                    output.flush()
                }
            }

            val totalElapsed = (System.currentTimeMillis() - downloadStartTime).coerceAtLeast(1L)
            val finalAverageSpeed = (totalRead * 1000L) / totalElapsed
            onProgress(totalRead, totalBytes, finalAverageSpeed)

            AppLogger.i(TAG, "Successfully downloaded artifact ZIP to ${outputZipFile.absolutePath} ($totalRead bytes in ${totalElapsed}ms, avg ${finalAverageSpeed / 1024} KB/s)")
            Result.success(outputZipFile)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error downloading artifact ZIP", e)
            if (outputZipFile.exists()) {
                outputZipFile.delete()
            }
            Result.failure(e)
        }
    }

    private fun parseIsoDate(isoString: String): Long {
        if (isoString.isBlank()) return 0L
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(isoString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
