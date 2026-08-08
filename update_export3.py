import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

# I want to replace the finishExport lambda.
start_str = "val finishExport = {"
end_str = "webView.webViewClient"

start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx)

new_block = """val finishExport = {
                        if (!executed) {
                            executed = true
                            try {
                                val printAdapter = webView.createPrintDocumentAdapter("default")
                                val printAttributes = android.print.PrintAttributes.Builder()
                                    .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                                    .setResolution(android.print.PrintAttributes.Resolution("id", "id", 300, 300))
                                    .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                                    .build()

                                val pfd = android.os.ParcelFileDescriptor.open(
                                    pdfFile,
                                    android.os.ParcelFileDescriptor.MODE_READ_WRITE or android.os.ParcelFileDescriptor.MODE_CREATE or android.os.ParcelFileDescriptor.MODE_TRUNCATE
                                )

                                printAdapter.onLayout(
                                    null,
                                    printAttributes,
                                    android.os.CancellationSignal(),
                                    object : android.print.PrintDocumentAdapter.LayoutResultCallback() {
                                        override fun onLayoutFinished(info: android.print.PrintDocumentInfo?, changed: Boolean) {
                                            printAdapter.onWrite(
                                                arrayOf(android.print.PageRange.ALL_PAGES),
                                                pfd,
                                                android.os.CancellationSignal(),
                                                object : android.print.PrintDocumentAdapter.WriteResultCallback() {
                                                    override fun onWriteFinished(pages: Array<out android.print.PageRange>?) {
                                                        super.onWriteFinished(pages)
                                                        try {
                                                            pfd.close()
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                        val displayPath = if (documentsDir.absolutePath.contains("Android/data")) {
                                                            "Saved to App Files/Documents/aipdfs/$safeName/"
                                                        } else {
                                                            "Saved to Documents/aipdfs/$safeName/"
                                                        }
                                                        Toast.makeText(context, displayPath, Toast.LENGTH_LONG).show()
                                                        onComplete(pdfFile, htmlFile)
                                                    }

                                                    override fun onWriteFailed(error: CharSequence?) {
                                                        super.onWriteFailed(error)
                                                        try { pfd.close() } catch (e: Exception) {}
                                                        Toast.makeText(context, "PDF Error: $error", Toast.LENGTH_LONG).show()
                                                        onComplete(pdfFile, htmlFile)
                                                    }
                                                }
                                            )
                                        }

                                        override fun onLayoutFailed(error: CharSequence?) {
                                            super.onLayoutFailed(error)
                                            try { pfd.close() } catch (e: Exception) {}
                                            Toast.makeText(context, "PDF Layout Error: $error", Toast.LENGTH_LONG).show()
                                            onComplete(pdfFile, htmlFile)
                                        }
                                    },
                                    null
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "PDF Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                onComplete(pdfFile, htmlFile)
                            }
                        }
                    }
                    
                    """

if start_idx != -1:
    content = content[:start_idx] + new_block + content[end_idx:]
    with open('/app/applet/app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
        f.write(content)
    print("Replaced!")
else:
    print("Not found")
