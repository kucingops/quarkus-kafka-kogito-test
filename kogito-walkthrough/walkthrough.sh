#!/usr/bin/env bash
# Tur Kogito (butuh curl dan jq): menjalankan contoh tiap langkah secara berurutan terhadap aplikasi yang sedang
# berjalan (mvn quarkus:dev). Tekan Enter untuk lanjut ke langkah berikutnya.
#
#   ./walkthrough.sh              interaktif
#   ./walkthrough.sh --no-pause   jalan terus tanpa menunggu Enter
#   BASE=http://host:port ./walkthrough.sh
set -euo pipefail

BASE="${BASE:-http://localhost:8082}"
PAUSE=true
[[ "${1:-}" == "--no-pause" ]] && PAUSE=false
OFFICER='user=ani&group=credit-officers'

command -v jq >/dev/null || { echo "Skrip ini butuh jq (https://jqlang.github.io/jq/)"; exit 1; }
pretty() { jq .; }
jf() { jq -r "$1"; }

step() { echo; echo "================================================================"; echo "$1"; echo "================================================================"; }
pause() { if $PAUSE; then read -r -p "-- Enter untuk lanjut --" _; fi; }
post() { echo "> POST $1" >&2; curl -sf -X POST "$BASE$1" -H 'Content-Type: application/json' -d "$2"; }
get() { echo "> GET $1" >&2; curl -sf "$BASE$1"; }

curl -sf "$BASE/q/health/ready" >/dev/null || { echo "Aplikasi belum jalan di $BASE. Jalankan dulu: mvn quarkus:dev"; exit 1; }

complete_task() { # processInstanceId approved note
  local tasks task_id
  for _ in $(seq 1 20); do
    tasks=$(get "/usertasks/instance?$OFFICER" 2>/dev/null)
    task_id=$(echo "$tasks" | jf "map(select(.processInfo.processInstanceId == \"$1\"))[0].id // empty")
    [[ -n "$task_id" ]] && break
    sleep 0.2
  done
  echo "Task ditemukan: $task_id"
  post "/usertasks/instance/$task_id/transition?$OFFICER" '{"transitionId":"claim"}' | jf '.status.name'
  post "/usertasks/instance/$task_id/transition?$OFFICER" "{\"transitionId\":\"complete\",\"data\":{\"approved\":$2,\"note\":\"$3\"}}" | jf '.status.name'
}

step "Langkah 1: Hello Kogito (Script Task, REST yang di-generate)"
id=$(post /hello '{"name":"Hilmi"}' | tee /dev/stderr | jf .id); echo
get "/journal/$id" | pretty
pause

step "Langkah 2: Service Task dan Gateway"
for score in 750 650 450; do
  post /loan_routing "{\"applicant\":{\"name\":\"Budi\",\"age\":30,\"monthlyIncome\":10000000,\"requestedAmount\":30000000,\"creditScore\":$score}}" \
    | jf '"creditScore '"$score"' -> score \(.score), route \(.route)"'
done
pause

step "Langkah 3: User Task"
id=$(post /loan_approval '{"applicant":{"name":"Citra","age":28,"monthlyIncome":8000000,"requestedAmount":40000000,"creditScore":680}}' | jf .id)
echo "Process instance $id menunggu review."
get "/loan_approval/$id" | pretty
complete_task "$id" true "Dokumen lengkap"
get "/journal/$id" | jf '.variables'
pause

step "Langkah 4: DMN (LoanEligibility)"
post /LoanEligibility '{"Applicant":{"name":"Dewi","age":30,"monthlyIncome":10000000,"creditScore":650},"Requested Amount":50000000}' | pretty
pause

step "Langkah 5: DMN di dalam proses + Call Activity"
id=$(post /loan_decision '{"applicant":{"name":"Hana","age":30,"monthlyIncome":10000000,"requestedAmount":80000000,"creditScore":650}}' | tee /dev/stderr | jf .id); echo
child=$(get /loan_approval | jf 'map(select(.applicant.name == "Hana"))[-1].id')
echo "DMN menjawab REVIEW, subproses loan_approval: $child"
complete_task "$child" true "Disetujui komite"
get "/journal/$id" | jf '.variables'
pause

step "Langkah 6: Rule DRL"
post /find-alerts '{"applicants":[{"name":"Mira","age":19,"monthlyIncome":5000000,"requestedAmount":50000000,"creditScore":500},{"name":"Nanda","age":30,"monthlyIncome":10000000,"requestedAmount":20000000,"creditScore":720}]}' | pretty
pause

step "Langkah 7: Signal dan Timer"
id=$(post /loan_documents '{"applicant":{"name":"Oki"},"timeout":"PT1H"}' | jf .id)
post "/loan_documents/$id/documentsUploaded" 'KTP, NPWP, slip gaji' >/dev/null
get "/journal/$id" | jf '.variables.status'
id=$(post /loan_documents '{"applicant":{"name":"Oki"},"timeout":"PT2S"}' | jf .id)
echo "Menunggu timer 2 detik..."; sleep 3
get "/journal/$id" | jf '.variables.status'
pause

step "Langkah 8: Error dan Kompensasi"
for amount in 50000000 150000000; do
  id=$(post /loan_disbursement "{\"applicant\":{\"name\":\"Raka\",\"age\":30,\"monthlyIncome\":100000000,\"requestedAmount\":$amount,\"creditScore\":750}}" | jf .id)
  get "/journal/$id" | jf '"\(.variables.applicant.requestedAmount): \(.variables.status)  nodes=\(.nodes | join(" > "))"'
done
echo; echo "Selesai. Lihat log aplikasi untuk urutan RESERVE / TRANSFER / RELEASE."
