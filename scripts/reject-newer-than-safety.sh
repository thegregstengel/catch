#!/bin/sh
# Exit 1 when Catch 2026.11.02 and earlier would rank this version above the
# safety release (2026.11.03.2). Those tablets install the single highest
# stable version they can parse, so a higher version skips the safety release.
#
# A version the old parser does not understand is ignored (it never wins), so
# this script allows it. The release workflow still only accepts Catch tags.
set -eu

if [ "$#" -ne 1 ]; then
  echo "usage: reject-newer-than-safety.sh <version>" >&2
  exit 2
fi

VERSION=$1
ROOT=$(cd "$(dirname "$0")/.." && pwd)

python3 - "$VERSION" "$ROOT/android/app/src/main/java/app/catchrower/update/SafetyRelease.kt" <<'PY'
import re, sys

version = sys.argv[1]
if version.startswith("v"):
    version = version[1:]
safety_src = open(sys.argv[2], encoding="utf-8").read()
match = re.search(r'const val VERSION = "([^"]+)"', safety_src)
if not match:
    sys.stderr.write("SafetyRelease.VERSION is missing\n")
    sys.exit(2)
safety = match.group(1)

pattern = re.compile(
    r"^(\d{4})\.(\d{1,2})\.(\d{1,2})(?:\.(\d))?(?:-(dev|(?:alpha|beta|rc)\.(\d{1,2})))?$"
)

def code(name):
    found = pattern.fullmatch(name.strip())
    if not found:
        return None
    year, month, day, same, suffix, pre = found.groups()
    if suffix == "dev":
        pre_release = 0
    elif not suffix:
        pre_release = 99
    else:
        pre_release = max(1, min(98, int(pre)))
    date = (int(year) - 2000) * 10000 + int(month) * 100 + int(day)
    same_day = int(same) if same else 1
    return date * 1000 + same_day * 100 + pre_release

candidate = code(version)
ceiling = code(safety)
if ceiling is None:
    sys.stderr.write("safety version %s does not parse\n" % safety)
    sys.exit(2)
# Missing code sorts last on the old updater, so it cannot replace the safety release.
if candidate is None or candidate <= ceiling:
    sys.exit(0)
sys.stderr.write(
    "%s sorts above %s for tablets on 2026.11.02 and earlier\n" % (version, safety)
)
sys.exit(1)
PY
