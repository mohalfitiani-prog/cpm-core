#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
app_dir="$repo_root/flutter_app"
if [ ! -d "$app_dir/android" ]; then
  scaffold_dir="$(mktemp -d)"
  flutter create --platforms=android --project-name=qtwxbp --org=com.aistudio.cpdms "$scaffold_dir/qtwxbp"
  cp -R "$scaffold_dir/qtwxbp/android" "$app_dir/android"
  cp "$scaffold_dir/qtwxbp/.metadata" "$app_dir/.metadata"
fi
python3 - "$app_dir" <<'PY'
import pathlib,sys
root=pathlib.Path(sys.argv[1])
p=root/'android/app/src/main/AndroidManifest.xml'
s=p.read_text().replace('android:label="qtwxbp"','android:label="CPM Core"')
if 'android.permission.INTERNET' not in s:s=s.replace('<application','<uses-permission android:name="android.permission.INTERNET"/>\n    <application',1)
p.write_text(s)
p=root/'android/app/build.gradle.kts';s=p.read_text().replace('minSdk = flutter.minSdkVersion','minSdk = 23');p.write_text(s)
PY
