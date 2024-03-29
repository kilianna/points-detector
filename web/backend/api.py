
import json
import api_files
import api_run

from typing import Any
from handler import MyHTTPRequestHandler
from threading import Thread

def api_get_shutdown(handler: MyHTTPRequestHandler, args: dict[str, Any]):
    thread = Thread(target=lambda s: s.shutdown(), args=(handler.server, ))
    thread.daemon = True
    thread.start()
    handler.send_simple_data(200, { 'status': 'OK' })

def api_get_reset(handler: MyHTTPRequestHandler, args: dict[str, Any]):
    api_files.reset()
    handler.send_simple_data(200, { 'status': 'OK' })

api = [
    api_get_shutdown,
    api_get_reset,

    api_files.api_get_upload_init,
    api_files.api_post_upload_chunk,
    api_files.api_get_upload_save,
    api_files.api_get_download_info,
    api_files.api_get_download_chunk,

    api_run.api_get_run,
    api_run.api_post_run,
]
