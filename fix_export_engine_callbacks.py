import sys

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

target1 = """                                        object : android.print.PrintDocumentAdapter.LayoutResultCallback() {"""
replacement1 = """                                        android.print.CustomLayoutResultCallback(object : android.print.CustomLayoutResultCallback.Callback {
                                            override fun onLayoutCancelled() {}"""

target2 = """                                        object : android.print.PrintDocumentAdapter.WriteResultCallback() {"""
replacement2 = """                                        android.print.CustomWriteResultCallback(object : android.print.CustomWriteResultCallback.Callback {
                                            override fun onWriteCancelled() {}"""

target1_end = """                                            override fun onLayoutFailed(error: CharSequence?) {
                                                super.onLayoutFailed(error)
                                                onComplete(null, htmlFile)
                                            }
                                        },"""
replacement1_end = """                                            override fun onLayoutFailed(error: CharSequence?) {
                                                onComplete(null, htmlFile)
                                            }
                                        }),"""

target2_end = """                                                            override fun onWriteFinished(pages: Array<android.print.PageRange>) {
                                                                super.onWriteFinished(pages)
                                                                onComplete(pdfFile, htmlFile)
                                                            }
                                                            override fun onWriteFailed(error: CharSequence?) {
                                                                super.onWriteFailed(error)
                                                                onComplete(null, htmlFile)
                                                            }
                                                        }
                                                    )"""
replacement2_end = """                                                            override fun onWriteFinished(pages: Array<android.print.PageRange>) {
                                                                onComplete(pdfFile, htmlFile)
                                                            }
                                                            override fun onWriteFailed(error: CharSequence?) {
                                                                onComplete(null, htmlFile)
                                                            }
                                                        })
                                                    )"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)
content = content.replace(target1_end, replacement1_end)
content = content.replace(target2_end, replacement2_end)

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
    f.write(content)
print("Updated to use CustomLayoutResultCallback")
