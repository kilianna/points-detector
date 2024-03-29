
from pathlib import Path

root = Path(__file__).parent.parent.parent
auth_key = (Path(__file__).parent.parent / 'private/key.txt').read_text().strip()

