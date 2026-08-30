import re
with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

old_latex = r'''                \\documentclass\{article\}
                \\usepackage\{amsmath\}
                \\usepackage\{amsfonts\}
                \\usepackage\{amssymb\}
                \\usepackage\{fontspec\}
                \\usepackage\[Bengali\]\{ucharclasses\}
                \\setmainfont\{DejaVu Serif\}
                \\newfontfamily\\bengalifont\[
                    Path=/data/data/com.termux/files/home/,
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                \]\{solaiman.ttf\}
                \\setTransitionsFor\{Bengali\}\{\\bengalifont\}\{\}
                \\setTransitionsFor\{Devanagari\}\{\\bengalifont\}\{\}
                \\setTransitionsFor\{BasicLatin\}\{\\rmfamily\}\{\}'''

new_latex = r'''                \\documentclass{article}
                \\usepackage{amsmath}
                \\usepackage{amsfonts}
                \\usepackage{amssymb}
                \\usepackage{fontspec}
                \\usepackage{ucharclasses} % Removed invalid [Bengali] option
                \\usepackage{tikz} % Added for diagrams
                \\usepackage{circuitikz} % Added for circuits
                \\usepackage{booktabs} % Added for toprule/midrule
                \\usepackage{tcolorbox} % Added for rectbox fallback
                \\setmainfont{DejaVu Serif}
                \\newfontfamily\\bengalifont[
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                ]{solaiman.ttf} % Removed hardcoded Termux path
                \\setTransitionsFor{Bengali}{\\bengalifont}{}
                \\setTransitionsFor{Devanagari}{\\bengalifont}{}
                \\setTransitionsFor{BasicLatin}{\\rmfamily}{}'''

# We need a safer replace
content = re.sub(old_latex, new_latex, content)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
