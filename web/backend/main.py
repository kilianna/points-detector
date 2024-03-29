#!/usr/bin/env python3

import re
import traceback
import urllib
from pathlib import Path
from threading import Thread
from http.server import HTTPServer, SimpleHTTPRequestHandler
from api import api
from auth import auth_key
from handler import MyHTTPRequestHandler

def main():
    with HTTPServer(('localhost', 39897), MyHTTPRequestHandler) as server:
        server._api = api
        print(f'URL: http://localhost:{server.server_port}/#_auth_{auth_key}')
        server.serve_forever()

if __name__ == '__main__':
    exit(main() or 0)
