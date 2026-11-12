#!/bin/bash
# Independent checks of a durability bench run, with tools that share no code with the bench:
#   Elle (Jepsen) on signals and queries as a list-append history
#   Porcupine on updates as a fetch-and-increment counter (linearizability)
# usage: independent/check.sh <run-dir>     prints one line per check; exit 1 if any fails
set -u
RUN="$(cd "$1" && pwd)"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
STATUS=0
ELLE=$(cd "$ROOT" && ./gradlew -q -p native-examples/independent/elle run --args="$RUN" 2>/dev/null | tail -1)
field() { echo "$ELLE" | grep -o ":$1 {:valid? [a-z]*, :anomaly-types [^,]*" | sed -E 's/.*:valid\? ([a-z]*), :anomaly-types (.*)/\1 \2/'; }
SER=$(field all-ops-serializable); FIN=$(field signals-and-final-reads-strict-serializable); STRICT=$(field all-ops-strict-serializable)
echo "elle serializable (all signals and queries):          ${SER:-error}"
echo "elle strict-serializable (signals and final reads):   ${FIN:-error}"
echo "elle strict-serializable (all; lag expected):         ${STRICT:-error}"
case "$SER" in true*) ;; *) STATUS=1;; esac
case "$FIN" in true*) ;; *) STATUS=1;; esac
PORC=$(cd "$HERE/porcupine" && go run . "$RUN" 2>&1 | grep PORCUPINE)
echo "porcupine linearizable updates:                      ${PORC#PORCUPINE: }"
case "$PORC" in *": linearizable") ;; *) STATUS=1;; esac
exit $STATUS
