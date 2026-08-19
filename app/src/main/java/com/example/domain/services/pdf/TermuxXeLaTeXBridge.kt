package com.example.domain.services.pdf

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

object TermuxXeLaTeXBridge {

    private const val TERMUX_PACKAGE = "com.termux"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    private val executionCounter = AtomicInteger(1000)

    private val pendingJobs =
        ConcurrentHashMap<Int, kotlinx.coroutines.CancellableContinuation<CommandResult>>()

    data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
        val errorMessage: String?
    )

    suspend fun compile(
        context: Context,
        texFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(texFile.exists()) {
                "LaTeX file does not exist: ${texFile.absolutePath}"
            }

            val outputDir = texFile.parentFile
                ?: throw IllegalArgumentException(
                    "LaTeX file has no parent directory"
                )

            val executionId = executionCounter.getAndIncrement()

            val resultIntent = Intent(
                context,
                TermuxCommandResultService::class.java
            ).apply {
                putExtra(
                    TermuxCommandResultService.EXTRA_EXECUTION_ID,
                    executionId
                )
            }

            val pendingIntent = PendingIntent.getService(
                context,
                executionId,
                resultIntent,
                PendingIntent.FLAG_ONE_SHOT or
                    PendingIntent.FLAG_MUTABLE
            )

            val intent = Intent().apply {
                setClassName(
                    TERMUX_PACKAGE,
                    "com.termux.app.RunCommandService"
                )

                action = ACTION_RUN_COMMAND

                putExtra(
                    "com.termux.RUN_COMMAND_PATH",
                    "/data/data/com.termux/files/usr/bin/xelatex"
                )

                putExtra(
                    "com.termux.RUN_COMMAND_ARGUMENTS",
                    arrayOf(
                        "-interaction=nonstopmode",
                        "-halt-on-error",
                        texFile.absolutePath
                    )
                )

                putExtra(
                    "com.termux.RUN_COMMAND_WORKDIR",
                    outputDir.absolutePath
                )

                putExtra(
                    "com.termux.RUN_COMMAND_BACKGROUND",
                    true
                )

                putExtra(
                    "com.termux.RUN_COMMAND_LABEL",
                    "XeLaTeX PDF compilation"
                )

                putExtra(
                    "com.termux.RUN_COMMAND_DESCRIPTION",
                    "Compiles the generated LaTeX document with XeLaTeX."
                )

                putExtra(
                    "com.termux.RUN_COMMAND_PENDING_INTENT",
                    pendingIntent
                )
            }

            val result = suspendCancellableCoroutine<CommandResult> { continuation ->
                pendingJobs[executionId] = continuation

                continuation.invokeOnCancellation {
                    pendingJobs.remove(executionId)
                }

                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                } catch (e: Exception) {
                    pendingJobs.remove(executionId)
                    continuation.resume(
                        CommandResult(
                            exitCode = -1,
                            stdout = "",
                            stderr = "",
                            errorMessage = e.message
                        )
                    )
                }
            }

            if (result.exitCode != 0) {
                throw Exception(
                    result.errorMessage
                        ?: result.stderr.ifBlank {
                            "XeLaTeX failed with exit code ${result.exitCode}"
                        }
                )
            }

            val pdfFile = File(
                outputDir,
                texFile.nameWithoutExtension + ".pdf"
            )

            require(pdfFile.exists()) {
                "XeLaTeX completed successfully but PDF was not created."
            }

            pdfFile
        }
    }

    internal fun deliverResult(
        executionId: Int,
        result: CommandResult
    ) {
        pendingJobs.remove(executionId)?.resume(result)
    }
}

class TermuxCommandResultService : IntentService(
    "TermuxCommandResultService"
) {

    companion object {
        const val EXTRA_EXECUTION_ID = "execution_id"

        private const val RESULT_BUNDLE =
            "result_bundle"

        private const val EXIT_CODE =
            "exitCode"

        private const val STDOUT =
            "stdout"

        private const val STDERR =
            "stderr"

        private const val ERROR_MESSAGE =
            "err"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val executionId =
            intent.getIntExtra(EXTRA_EXECUTION_ID, -1)

        if (executionId == -1) return

        val resultBundle =
            intent.getBundleExtra(RESULT_BUNDLE)

        if (resultBundle == null) {
            TermuxXeLaTeXBridge.deliverResult(
                executionId,
                TermuxXeLaTeXBridge.CommandResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "",
                    errorMessage = "Termux returned no result bundle."
                )
            )
            return
        }

        val exitCode =
            resultBundle.getInt(EXIT_CODE, -1)

        val stdout =
            resultBundle.getString(STDOUT, "")

        val stderr =
            resultBundle.getString(STDERR, "")

        val errorMessage =
            resultBundle.getString(ERROR_MESSAGE)

        TermuxXeLaTeXBridge.deliverResult(
            executionId,
            TermuxXeLaTeXBridge.CommandResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                errorMessage = errorMessage
            )
        )
    }
}
