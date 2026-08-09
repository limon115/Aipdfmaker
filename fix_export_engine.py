import sys

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

target = """                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                                    val printAdapter = webView.createPrintDocumentAdapter(safeName)
                                    val jobName = "${context.getString(com.example.R.string.app_name)} Document - $safeName"
                                    
                                    printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS).build())
                                    
                                    onComplete(null, htmlFile)"""

replacement = """                                    val printAdapter = webView.createPrintDocumentAdapter(safeName)
                                    val pdfFile = File(baseDir, "${safeName}.pdf")
                                    val attributes = android.print.PrintAttributes.Builder()
                                        .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                                        .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                                        .setResolution(android.print.PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                                        .build()

                                    printAdapter.onLayout(
                                        null,
                                        attributes,
                                        android.os.CancellationSignal(),
                                        object : android.print.PrintDocumentAdapter.LayoutResultCallback() {
                                            override fun onLayoutFinished(info: android.print.PrintDocumentInfo, changed: Boolean) {
                                                try {
                                                    val pfd = android.os.ParcelFileDescriptor.open(
                                                        pdfFile,
                                                        android.os.ParcelFileDescriptor.MODE_READ_WRITE or 
                                                        android.os.ParcelFileDescriptor.MODE_CREATE or 
                                                        android.os.ParcelFileDescriptor.MODE_TRUNCATE
                                                    )
                                                    printAdapter.onWrite(
                                                        arrayOf(android.print.PageRange.ALL_PAGES),
                                                        pfd,
                                                        android.os.CancellationSignal(),
                                                        object : android.print.PrintDocumentAdapter.WriteResultCallback() {
                                                            override fun onWriteFinished(pages: Array<android.print.PageRange>) {
                                                                super.onWriteFinished(pages)
                                                                onComplete(pdfFile, htmlFile)
                                                            }
                                                            override fun onWriteFailed(error: CharSequence?) {
                                                                super.onWriteFailed(error)
                                                                onComplete(null, htmlFile)
                                                            }
                                                        }
                                                    )
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    onComplete(null, htmlFile)
                                                }
                                            }
                                            
                                            override fun onLayoutFailed(error: CharSequence?) {
                                                super.onLayoutFailed(error)
                                                onComplete(null, htmlFile)
                                            }
                                        },
                                        null
                                    )"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
        f.write(content)
    print("ExportEngine updated successfully")
else:
    print("Target not found in ExportEngine")
