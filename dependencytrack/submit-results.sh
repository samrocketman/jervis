#!/bin/bash
set -eo pipefail
trap 'echo "${REASON:-}Did you follow dependencytrack/README.md?" >&2' ERR

#pre-flight checks
REASON='ERROR: This must be run relative to repository root.  '
[ -d .git ]
REASON='ERROR: missing bom.json.  '
[ -f build/reports/bom.json ]
REASON='ERROR: Missing DT_API_TOKEN env variable.  '
[ -n "${DT_API_TOKEN:-}" ]
unset REASON

DT_PROJECT="${DT_PROJECT:-jervis}"
DT_VERSION="$(awk -F= '$1 == "version" { print $2; exit }' gradle.properties)"
cat > build/reports/payload.json <<EOF
{
  "projectName": "${DT_PROJECT}",
  "projectVersion": "${DT_VERSION}",
  "autoCreate": true,
  "bom": "$( openssl enc -base64 -A -in build/reports/bom.json )"
}
EOF
curl -X PUT \
  -H 'Content-Type: application/json' \
  -H "X-Api-Key: ${DT_API_TOKEN}" \
  -d @build/reports/payload.json \
  http://localhost:8081/api/v1/bom
