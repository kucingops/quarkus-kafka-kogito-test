#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
DIR="$(cd "$(dirname "$0")" && pwd)"

while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" ]] && continue
  curl -s -o /dev/null -w "%{http_code}  " -X POST "$BASE_URL/transactions/publish" \
       -H "Content-Type: application/json" --data-raw "$line"
  echo "${line:0:80}"
done < "$DIR/sample-transactions.jsonl"
