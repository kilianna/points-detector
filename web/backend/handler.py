
import re
import json
import traceback
import urllib
from pathlib import Path
from auth import auth_key
from collections.abc import Callable
from typing import Any

from http.server import SimpleHTTPRequestHandler

class MyHTTPRequestHandler(SimpleHTTPRequestHandler):

    def __init__(self, request, client_address, server):
        super().__init__(request, client_address, server, directory=Path(__file__).parent / '../dist')

    def send_simple_data(self, code: int, data: 'str|bytes'):
        if isinstance(data, str):
            mime = 'text/plain; charset=utf-8'
            data = data.encode('UTF-8')
        elif isinstance(data, bytes) or isinstance(data, bytearray):
            mime = 'application/octet-stream'
        else:
            mime = 'application/json'
            data = json.dumps(data).encode('UTF-8')
        self.send_response(code)
        self.send_header('Pragma', 'no-cache')
        self.send_header('Cache-Control', 'no-store, no-cache, max-age=0, must-revalidate, proxy-revalidate')
        self.send_header('Content-type', mime)
        self.send_header('Content-Length', len(data))
        self.end_headers()
        self.wfile.write(data)

    def send_json_ok(self, **kwargs):
        data = dict(kwargs)
        data['status'] = 'OK'
        self.send_simple_data(200, data)

    def send_json_error(self, message: str, **kwargs):
        data = dict(kwargs)
        data['status'] = 'ERROR'
        data['message'] = message
        self.send_simple_data(200, data)

    def get_api_method(self, prefix: str) -> 'tuple[Callable, dict[str, Any]]':
        if self.path.startswith('/_api/'):
            url = urllib.parse.urlparse(self.path)
            name = prefix + url.path[6:].replace('-', '_').replace('/', '_')
            args = json.loads(urllib.parse.unquote(url.query))
            if ('_auth_' in args) and (args['_auth_'] == auth_key):
                for func in self.server._api:
                    if func.__name__ == name:
                        return (func, args)
        return (None, None)

    def do_GET(self):
        func, args = self.get_api_method('api_get_')
        if func is None:
            super().do_GET()
        else:
            try:
                func(self, args)
            except Exception as ex:
                self.send_simple_data(500, traceback.format_exc())

    def do_POST(self):
        func, args = self.get_api_method('api_post_')
        if func is None:
            super().do_POST()
        else:
            try:
                content_len = int(self.headers.get('Content-Length', 0))
                func(self, args, content_len)
            except Exception as ex:
                self.send_simple_data(500, traceback.format_exc())

    def __x_log_message(self, format, *args, **kwargs):
        pass

