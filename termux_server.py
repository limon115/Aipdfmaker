import json
import subprocess
import os
import tempfile
from http.server import BaseHTTPRequestHandler, HTTPServer

class LatexCompilerHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == '/compile':
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)

            try:
                data = json.loads(post_data.decode('utf-8'))
                latex_str = data.get('latex', '')
                # Accept both 'fix_script' (from our earlier app patch) and 'fixer_script'
                fix_script = data.get('fix_script', data.get('fixer_script', ''))
            except json.JSONDecodeError:
                self._send_error(400, "Invalid JSON payload")
                return

            with tempfile.TemporaryDirectory() as tempdir:
                tex_path = os.path.join(tempdir, 'main.tex')
                
                # 1. Write the original LaTeX file
                with open(tex_path, 'w', encoding='utf-8') as f:
                    f.write(latex_str)

                # 2. If AI generated a fix script, execute it first against main.tex
                if fix_script:
                    print("🔧 Applying AI fixes via Python script...")
                    script_path = os.path.join(tempdir, 'apply_fixes.py')
                    with open(script_path, 'w', encoding='utf-8') as f:
                        f.write(fix_script)
                    
                    # Run the script and pass 'main.tex' as the target argument
                    fix_process = subprocess.run(
                        ['python3', 'apply_fixes.py', 'main.tex'],
                        cwd=tempdir,
                        capture_output=True,
                        text=True
                    )
                    
                    if fix_process.returncode != 0:
                        print("❌ AI Fix script failed.")
                        self._send_error(400, f"AI Fix Script Error:\n{fix_process.stdout}\n{fix_process.stderr}")
                        return
                    
                    print("✅ AI Fixes applied successfully.")

                # 3. Compile the LaTeX (either original, or modified by the script)
                print("⚙️ Compiling LaTeX (Pass 1)...")
                subprocess.run(
                    ['xelatex', '-interaction=nonstopmode', 'main.tex'],
                    cwd=tempdir,
                    capture_output=True,
                    text=True
                )
                print("⚙️ Compiling LaTeX (Pass 2)...")
                process = subprocess.run(
                    ['xelatex', '-interaction=nonstopmode', 'main.tex'],
                    cwd=tempdir,
                    capture_output=True,
                    text=True
                )

                pdf_path = os.path.join(tempdir, 'main.pdf')

                # Check if the PDF was generated
                if os.path.exists(pdf_path) and os.path.getsize(pdf_path) > 0:
                    print("✅ PDF generated successfully despite any warnings!")
                    with open(pdf_path, 'rb') as pdf_file:
                        pdf_bytes = pdf_file.read()

                    self.send_response(200)
                    self.send_header('Content-type', 'application/pdf')
                    self.end_headers()
                    self.wfile.write(pdf_bytes)
                else:
                    print("❌ Compilation completely failed.")
                    self._send_error(400, f"Compilation Error:\n{process.stdout}\n{process.stderr}")

    def _send_error(self, code, message):
        self.send_response(code)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()
        self.wfile.write(message.encode('utf-8'))

if __name__ == '__main__':
    server_address = ('127.0.0.1', 8080)
    httpd = HTTPServer(server_address, LatexCompilerHandler)
    print(f"🚀 Termux Localhost LaTeX Server running on http://{server_address[0]}:{server_address[1]}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    print("\n🛑 Server stopped.")
