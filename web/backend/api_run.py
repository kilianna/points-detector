
import json
import subprocess
from handler import MyHTTPRequestHandler
from auth import root
from typing import Any

def run(handler: MyHTTPRequestHandler, args: 'dict[str, Any]', input: 'bytes|None'):
    res = subprocess.run(args['args'],
                         input=input,
                         capture_output=True,
                         shell=(args['shell'] if 'shell' in args else False),
                         cwd=(args['cwd'] if 'cwd' in args else None)
                         )
    handler.send_json_ok(code=res.returncode, stdout=res.stdout.decode('latin'), stderr=res.stderr.decode('latin'))

def api_get_run(handler: MyHTTPRequestHandler, args: 'dict[str, Any]'):
    run(handler, args, None)

def api_post_run(handler: MyHTTPRequestHandler, args: 'dict[str, Any]', content_len: int):
    input = handler.rfile.read(content_len)
    run(handler, args, input)
