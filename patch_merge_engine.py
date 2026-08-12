import re

filepath = "app/src/main/java/com/example/domain/services/html/DocumentMergeEngine.kt"
with open(filepath, 'r') as f:
    code = f.read()

# Match the exact catch (e2: Exception) block and its contents
pattern = re.compile(r"catch\s*\(e2:\s*Exception\)\s*\{.*?Fallback 2:.*?\}", re.DOTALL)

# Replace it with the intelligent Regex extractor
replacement = r"""catch (e2: Exception) {
                    // 🛡️ SURGICAL FIX 3: Intelligent Regex Extraction
                    // Do not dump raw schema text. Extract only the readable content.
                    val contentRegex = Regex("\"(?:text|latex|value)\"\\s*:\\s*\"(.*?)\"")
                    val matches = contentRegex.findAll(safeJson)
                    
                    if (matches.any()) {
                        val extracted = matches.joinToString(" ") { it.groupValues[1] }
                        mergedBlocks.add(ParagraphBlock(text = extracted))
                    } else {
                        mergedBlocks.add(ParagraphBlock(text = "⚠️ [Data Extraction Error] The AI generated invalid mathematical JSON that could not be parsed."))
                    }
                }"""

new_code = pattern.sub(replacement, code)

with open(filepath, 'w') as f:
    f.write(new_code)
print("✅ DocumentMergeEngine patched: Intelligent fallback injected to stop JSON leaks.")
