#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
fixture=$(mktemp -d "${TMPDIR:-/tmp}/epiphany-clio-live.XXXXXX")
cleanup() {
  local active
  active=$(jobs -pr)
  if [[ -n "$active" ]]; then kill $active 2>/dev/null || true; fi
  wait 2>/dev/null || true
  rm -rf "$fixture"
}
trap cleanup EXIT
export EPIPHANY_EDN_DIR="$fixture/observations"
mkdir "$fixture/repo"
git -C "$fixture/repo" init -q
git -C "$fixture/repo" config user.name 'Epiphany local fixture'
git -C "$fixture/repo" config user.email 'fixture@example.invalid'
printf '# Clio archaeology\n\nThe orchard preserves inspectable research history.\n' > "$fixture/repo/notes.md"
git -C "$fixture/repo" add notes.md
git -C "$fixture/repo" commit -qm 'Create literal local search fixture'

clojure -M:run register --profile edn --request-id 00000000-0000-4000-8000-000000000123 "$fixture/repo"
clojure -M:run ingest --profile edn --index-dir "$fixture/index" "$fixture/repo"
clojure -M:run search --profile edn --mode lexical --format edn --index-dir "$fixture/index" orchard > "$fixture/search.edn"
grep -q 'notes.md' "$fixture/search.edn"
clojure -M:run ingest --profile edn --index-dir "$fixture/index" "$fixture/repo" > "$fixture/repeat.txt"
cat "$fixture/repeat.txt"
grep -Eq 'Revisions observed:[[:space:]]+0' "$fixture/repeat.txt"
grep -Eq 'Sections extracted:[[:space:]]+0' "$fixture/repeat.txt"

# Two independent JVMs concurrently append distinct valid observations into
# the same canonical ledger. The adapter owns read/decision/append serialization.
for ordinal in 1 2; do
  EPIPHANY_WRITER_ORDINAL="$ordinal" clojure -M -e '
    (require (quote [epiphany.infra.adapters.clio :as c]))
    (let [store (c/open-store (System/getenv "EPIPHANY_EDN_DIR"))
          port (c/make-observations-adapter store)
          n (System/getenv "EPIPHANY_WRITER_ORDINAL")]
      ((:record-repository-location! port)
       {:observation/id (random-uuid) :observation/request-id (random-uuid)
        :observation/type :repository/location-observed
        :observation/observed-at #inst "2026-09-12T00:00:00Z"
        :observation/adapter-version "live-fixture-v1" :observation/schema-version 1
        :resource-id (random-uuid)
        :repository/path {:path/raw (str "/literal-writer-" n)
                          :path/source :filesystem-argument :path/comparison :exact}
        :repository/common-git-dir {:path/raw (str "/literal-writer-" n "/.git")
                                    :path/source :filesystem-argument :path/comparison :exact}}))' &
  writers[$ordinal]=$!
done
wait "${writers[1]}"
wait "${writers[2]}"
clojure -M -e '
  (require (quote [epiphany.infra.adapters.clio :as c]))
  (let [store (c/open-store (System/getenv "EPIPHANY_EDN_DIR"))
        paths (set (map #(get-in % [:repository/path :path/raw])
                        (c/list-repository-locations store)))]
    (assert (every? paths ["/literal-writer-1" "/literal-writer-2"]))
    (println "PASS: separate JVM replay, lexical search, incremental ingestion and concurrent durable writes."))'
