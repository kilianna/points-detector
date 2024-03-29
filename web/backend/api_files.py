
import uuid
import shutil

from typing import Any
from handler import MyHTTPRequestHandler
from auth import root

class Upload:
    id: str
    content: bytearray
    def __init__(self):
        self.id = str(uuid.uuid4())
        self.content = bytearray()

uploads: 'dict[Upload]' = {}

def reset():
    global uploads
    uploads = {}

def api_get_upload_init(handler: MyHTTPRequestHandler, args: 'dict[str, Any]'):
    global uploads
    upload = Upload()
    uploads[upload.id] = upload
    handler.send_simple_data(200, upload.id)
    handler.send_json_ok(id=upload.id)

def api_post_upload_chunk(handler: MyHTTPRequestHandler, args: 'dict[str, Any]', content_len: int):
    global uploads
    id = args['id']
    if id not in uploads:
        handler.send_simple_data(404, 'ERROR: Not found')
    uploads[id].content += handler.rfile.read(content_len)
    handler.send_json_ok(size=len(uploads[id].content))

def api_get_upload_save(handler: MyHTTPRequestHandler, args: 'dict[str, Any]'):
    global uploads
    id = args['id']
    dest = root / 'data' / args['path']
    if id not in uploads:
        handler.send_simple_data(404, 'ERROR: Not found')
    dest.write_bytes(uploads[id].content)
    del uploads[id]
    handler.send_json_ok()

def api_get_download_info(handler: MyHTTPRequestHandler, path: str, query: 'dict[str, list[str]]', *_):
    src = root / 'data' / query['path'][0]
    stat = src.stat()
    handler.send_json_ok(size=stat.st_size)

def api_get_download_chunk(handler: MyHTTPRequestHandler, path: str, query: 'dict[str, list[str]]', *_):
    src = root / 'data' / query['path'][0]
    stat = src.stat()
    begin = int(query['begin'][0]) if 'begin' in query else 0
    if (begin > stat.st_size) or (begin < 0):
        return handler.send_json_error(f'Read outside of file. Chunk offset {begin}, file size {stat.st_size}.')
    end = begin + int(query['size'][0]) if 'size' in query else stat.st_size
    if (end > stat.st_size) or (end < begin):
        return handler.send_json_error(f'Invalid chunk size.')
    with open(src, 'rb') as fd:
        fd.seek(begin)
        handler.send_simple_data(200, fd.read(end - begin))

def api_get_download(handler: MyHTTPRequestHandler, path: str, query: 'dict[str, list[str]]', *_):
    src = root / 'data' / query['path'][0]
    stat = src.stat()
    handler.send_response(200)
    handler.send_header('Content-type', 'application/octet-stream')
    handler.send_header('Pragma', 'no-cache')
    handler.send_header('Cache-Control', 'no-store, no-cache, max-age=0, must-revalidate, proxy-revalidate')
    handler.send_header('Content-length', stat.st_size)
    handler.end_headers()
    with open(src, 'rb') as fd:
        shutil.copyfileobj(fd, handler.wfile)
