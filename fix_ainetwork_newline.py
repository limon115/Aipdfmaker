with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

import re
fixed_user_prompt = r'val userPrompt = "LaTeX Code:\\n$latexCode\\n\\nCompiler Log:\\n$logContent"'

content = re.sub(r'val userPrompt = "LaTeX Code:.*?Compiler Log:.*?" \+ logContent', fixed_user_prompt, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)
