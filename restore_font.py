import re
with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

target = """                \\newfontfamily\\bengalifont[
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                ]{solaiman.ttf} % Removed hardcoded Termux path"""

replacement = """                \\newfontfamily\\bengalifont[
                    Path=/data/data/com.termux/files/home/,
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                ]{solaiman.ttf}"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
