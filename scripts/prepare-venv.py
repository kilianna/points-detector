#!/usr/bin/env python3

import sys
import subprocess
from pathlib import Path

this_dir = Path(__file__).parent
venv = this_dir / '../.venv'

subprocess.run([sys.executable, '-m', 'venv', venv], check=True)

pip = venv / 'pip.exe'
if not pip.exists(): pip = venv / 'bin/pip'
python = venv / 'python.exe'
if not python.exists(): python = venv / 'bin/python'

subprocess.run([pip, 'install', '-r', this_dir / 'requirements.txt'], check=True)
