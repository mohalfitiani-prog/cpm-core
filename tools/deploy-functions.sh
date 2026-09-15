#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"
python3 - <<'PY'
import pathlib,re
options=pathlib.Path('flutter_app/lib/firebase_options.dart').read_text()
project=re.search(r'projectId:\s*[\x27\x22]([^\x27\x22]+)', options)
key=re.search(r'apiKey:\s*[\x27\x22]([^\x27\x22]+)', options)
if not project or project[1]!='cpm-core' or not key:
    raise SystemExit('Firebase Android configuration does not match cpm-core')
p=pathlib.Path('functions/.env.cpm-core')
if p.exists():
    lines=p.read_text().splitlines()
else:
    lines=[]
lines=[line for line in lines if not line.startswith('CPM_WEB_API_KEY=')]
lines.append('CPM_WEB_API_KEY='+key[1])
p.write_text('\n'.join(lines)+'\n')
PY
npm install --prefix functions --ignore-scripts
npm test --prefix functions
FUNCTIONS_DISCOVERY_TIMEOUT=60000 npx --yes firebase-tools deploy --project cpm-core --only functions:cpm-flutter
