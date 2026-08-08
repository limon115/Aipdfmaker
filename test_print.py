import sys
code = """
                            try {
                                val printAdapter = webView.createPrintDocumentAdapter("default")
                                val printAttributes = android.print.PrintAttributes.Builder()
                                    .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                                    .setResolution(android.print.PrintAttributes.Resolution("id", "id", 300, 300))
                                    .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                                    .build()
"""
print("Can do this")
