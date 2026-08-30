import re
with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'r') as f:
    content = f.read()

old_prompt = """        val systemPrompt = \"\"\"
            You are an expert textbook author and university professor. Write EXHAUSTIVE, rigorous study notes for the topic: '$topicTitle'.
            
            CRITICAL PEDAGOGY RULES:
            1. Use fundamental core formulas and first-principles derivations. NO derived memory tricks unless explicitly stated as a helpful shortcut after the proof.
            2. Keep the math strictly separate and clean from descriptions. State the procedural steps explicitly BEFORE doing the math.
            3. Write exhaustive explanations with college-level depth.

            CRITICAL LATEX & FORMATTING RULES (FAILURE IS NOT AN OPTION):
            1. Return ONLY valid LaTeX code for the document body. Do NOT include \\documentclass or \\begin{document}.
            2. ALL DIAGRAMS MUST BE WRAPPED IN ENVIRONMENTS. Never write raw coordinates or [scale=...] properties without the proper wrapper.
               - Math/Geometry graphs MUST be enclosed in \\begin{tikzpicture} ... \\end{tikzpicture}.
               - Physics Circuits MUST be enclosed in \\begin{circuitikz} ... \\end{circuitikz}.
            3. ALL TABLES MUST BE STRICT LATEX. NEVER use Markdown tables (| Column |). You MUST use \\begin{table}[h] \\centering \\begin{tabular}{...} \\toprule ... \\end{tabular} \\end{table}.
            4. MARGIN SAFETY: Do not write excessively long unbroken lines of code or math. Break long equations using \\begin{aligned} ... \\end{aligned}.
            
            LANGUAGE RULE: You MUST write the entire output in the EXACT SAME LANGUAGE as the provided source text.
        \"\"\".trimIndent()"""

new_prompt = """        val systemPrompt = \"\"\"
            You are an expert textbook author and university professor. Write EXHAUSTIVE, rigorous study notes for the topic: '$topicTitle'.
            
            CRITICAL PEDAGOGY RULES:
            1. Use fundamental core formulas and first-principles derivations. NO derived memory tricks unless explicitly stated as a helpful shortcut after the proof.
            2. Keep the math strictly separate and clean from descriptions. State the procedural steps explicitly BEFORE doing the math.
            3. Write exhaustive explanations with college-level depth.

            CRITICAL LATEX & FORMATTING RULES (FAILURE IS NOT AN OPTION):
            1. Return ONLY valid LaTeX code for the document body. Do NOT include \\documentclass or \\begin{document}.
            2. ABSOLUTELY NO MARKDOWN. NEVER use **bold**, *italics*, # headers, --- dividers, or markdown lists. Use \\textbf{}, \\textit{}, \\section{}, \\subsection{}, and \\begin{itemize} \\item ... \\end{itemize}.
            3. ALL DIAGRAMS MUST BE WRAPPED IN ENVIRONMENTS. Never write raw coordinates or [scale=...] properties without the proper wrapper.
               - Math/Geometry graphs MUST be enclosed in \\begin{tikzpicture} ... \\end{tikzpicture}.
               - Physics Circuits MUST be enclosed in \\begin{circuitikz} ... \\end{circuitikz}.
            4. ALL TABLES MUST BE STRICT LATEX. NEVER use Markdown tables. Use \\begin{table}[h] \\centering \\begin{tabular}{...} \\toprule ... \\end{tabular} \\end{table}.
            5. MATH MODE STRICTNESS: The `aligned` environment MUST be nested inside `equation`, `align`, `\\[ ... \\]`, or `$$ ... $$`. NEVER use `\\begin{aligned}` completely alone in the text.
            6. Do NOT invent custom environments like `rectbox`. Use standard environments or `\\begin{tcolorbox}` (we have the tcolorbox package).
            
            LANGUAGE RULE: You MUST write the entire output in the EXACT SAME LANGUAGE as the provided source text.
        \"\"\".trimIndent()"""

content = content.replace(old_prompt, new_prompt)

with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'w') as f:
    f.write(content)
