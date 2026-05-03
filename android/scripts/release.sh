#!/usr/bin/env bash
set -euo pipefail

# Build a signed release AAB locally and optionally upload to Google Play.
#
# Usage:
#   scripts/release.sh <versionName> [versionCode]
#   scripts/release.sh 0.1.0 1
#   scripts/release.sh 0.1.0 1 --upload          # uploads to Play internal track
#   scripts/release.sh 0.1.0 1 --upload --track production
#
# Required files (kept out of git):
#   android/keystore.properties   storeFile=..., storePassword=..., keyAlias=..., keyPassword=...
#   android/play-service-account.json   (only if --upload is used)

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <versionName> [versionCode] [--upload] [--track <track>]"
  exit 1
fi

VERSION_NAME="$1"
shift

VERSION_CODE=""
UPLOAD=0
TRACK="internal"

if [[ $# -gt 0 && "$1" != --* ]]; then
  VERSION_CODE="$1"
  shift
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --upload) UPLOAD=1; shift ;;
    --track) TRACK="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

if ! [[ "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Error: versionName must be X.Y.Z (got '$VERSION_NAME')"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ANDROID_DIR"

if [[ ! -f keystore.properties ]]; then
  echo "Error: android/keystore.properties not found."
  echo "Create it with: storeFile=upload-keystore.jks, storePassword=..., keyAlias=..., keyPassword=..."
  exit 1
fi

if [[ -z "$VERSION_CODE" ]]; then
  VERSION_CODE=$(date +%s)
  echo "No versionCode given, using timestamp: $VERSION_CODE"
fi

if ! [[ "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
  echo "Error: versionCode must be a positive integer (got '$VERSION_CODE')"
  exit 1
fi

echo "Building AAB: versionName=$VERSION_NAME versionCode=$VERSION_CODE"
./gradlew clean bundleRelease \
  -PversionName="$VERSION_NAME" \
  -PversionCode="$VERSION_CODE"

AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
if [[ ! -f "$AAB_PATH" ]]; then
  echo "Error: build did not produce $AAB_PATH"
  exit 1
fi

OUT_DIR="release-artifacts"
mkdir -p "$OUT_DIR"
OUT_AAB="$OUT_DIR/muxy-$VERSION_NAME-$VERSION_CODE.aab"
cp "$AAB_PATH" "$OUT_AAB"
echo "AAB ready: $OUT_AAB"

if [[ $UPLOAD -eq 0 ]]; then
  echo "Done. Upload to Play Console manually, or re-run with --upload."
  exit 0
fi

if [[ ! -f play-service-account.json ]]; then
  echo "Error: android/play-service-account.json not found (required for --upload)."
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: python3 required for --upload."
  exit 1
fi

if ! python3 -c "import googleapiclient" >/dev/null 2>&1; then
  echo "Installing google-api-python-client into a local venv..."
  python3 -m venv .venv-play
  # shellcheck disable=SC1091
  source .venv-play/bin/activate
  pip install --quiet google-api-python-client google-auth
else
  if [[ -d .venv-play ]]; then
    # shellcheck disable=SC1091
    source .venv-play/bin/activate
  fi
fi

echo "Uploading $OUT_AAB to Play track: $TRACK"
python3 - "$OUT_AAB" "$TRACK" <<'PY'
import sys
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from google.oauth2 import service_account

aab_path, track = sys.argv[1], sys.argv[2]
package = "com.muxy.app"

creds = service_account.Credentials.from_service_account_file(
    "play-service-account.json",
    scopes=["https://www.googleapis.com/auth/androidpublisher"],
)
service = build("androidpublisher", "v3", credentials=creds)

edit = service.edits().insert(packageName=package, body={}).execute()
edit_id = edit["id"]

bundle = service.edits().bundles().upload(
    packageName=package,
    editId=edit_id,
    media_body=MediaFileUpload(aab_path, mimetype="application/octet-stream"),
).execute()
version_code = bundle["versionCode"]
print(f"Uploaded versionCode={version_code}")

service.edits().tracks().update(
    packageName=package,
    editId=edit_id,
    track=track,
    body={"releases": [{"versionCodes": [str(version_code)], "status": "draft"}]},
).execute()

service.edits().commit(packageName=package, editId=edit_id).execute()
print(f"Committed to track '{track}' as draft. Promote/publish from Play Console.")
PY

echo "Done."
