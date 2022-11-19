#!/bin/bash
set -eo pipefail
trap 'echo "${REASON:-}Did you follow dependencytrack/README.md?" >&2' ERR

#pre-flight checks
REASON='ERROR: This must be run relative to repository root.  '
[ -d .git ]
REASON='ERROR: missing bom.json.  '
[ -f build/reports/bom.json ]
#REASON='ERROR: Missing DT_API_TOKEN env variable.  '
#[ -n "${DT_API_TOKEN:-}" ]
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

if [ -z "${DT_ENDPOINT:-}" ]; then
  DT_ENDPOINT=http://localhost:8081
fi

headers=( -H 'Content-Type: application/json' )
if [ -n "${DT_API_TOKEN:-}" ]; then
  headers+=( -H "X-Api-Key: ${DT_API_TOKEN}" )
fi

REASON="ERROR: Upload failed is the DT_API_TOKEN or DT_ENDPOINT correct?  DT_ENDPOINT='${DT_ENDPOINT}'.  "
curl -X PUT \
  "${headers[@]}" \
  -d @build/reports/payload.json \
  "${DT_ENDPOINT%/}/api/v1/bom"
