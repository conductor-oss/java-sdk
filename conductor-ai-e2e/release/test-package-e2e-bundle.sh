#!/usr/bin/env bash
set -euo pipefail

# ── Validator for package-e2e-bundle.sh ──────────────────────────────────────
# Builds the bundle at a throwaway version and asserts:
#   - tarball exists and extracts to the expected dir
#   - carries an executable, syntactically-valid run.sh + README
#   - every test source from conductor-ai-e2e made it in (file-count parity)
#   - the SDK is pinned at the version, with no @VERSION@ placeholder left
#   - the Gradle wrapper is complete and gradlew is executable
# All checks are static + deterministic (no network, no live server, no
# compilation — the pinned SDK version need not exist on Maven Central).
# Run: ./conductor-ai-e2e/release/test-package-e2e-bundle.sh

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
VERSION="9.9.9-test"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }
pass() { echo "  ok: $*"; }

"$HERE/package-e2e-bundle.sh" --version "$VERSION" --out "$WORK/dist" >/dev/null

NAME="conductor-ai-e2e-java-$VERSION"
TAR="$WORK/dist/$NAME.tar.gz"

[[ -f "$TAR" ]] || fail "tarball not produced ($TAR)"
pass "tarball produced"

mkdir -p "$WORK/x"
tar -xzf "$TAR" -C "$WORK/x"
ROOT="$WORK/x/$NAME"
[[ -d "$ROOT" ]] || fail "tarball does not extract to $NAME/"
pass "extracts to $NAME/"

[[ -f "$ROOT/run.sh" ]] || fail "missing run.sh"
[[ -x "$ROOT/run.sh" ]] || fail "run.sh not executable"
bash -n "$ROOT/run.sh"  || fail "run.sh has a bash syntax error"
[[ -f "$ROOT/README.md" ]] || fail "missing README.md"
pass "run.sh + README present and valid"

# Every suite source made it into the bundle.
SRC_COUNT="$(ls "$REPO_ROOT"/conductor-ai-e2e/src/test/java/*.java | wc -l | tr -d ' ')"
BUNDLE_COUNT="$(ls "$ROOT"/src/test/java/*.java | wc -l | tr -d ' ')"
[[ "$SRC_COUNT" == "$BUNDLE_COUNT" ]] \
  || fail "source parity: repo has $SRC_COUNT test sources, bundle has $BUNDLE_COUNT"
pass "all $SRC_COUNT test sources present"

# SDK pinned at the packaged version, no unexpanded placeholders anywhere.
grep -q "org.conductoross:conductor-ai:" "$ROOT/build.gradle" \
  || fail "build.gradle does not pin org.conductoross:conductor-ai"
grep -q "'$VERSION'" "$ROOT/build.gradle" \
  || fail "build.gradle does not pin version $VERSION"
if grep -rn '@VERSION@' "$ROOT" >/dev/null 2>&1; then
  fail "unexpanded @VERSION@ placeholder left in bundle"
fi
pass "SDK pinned at $VERSION, no placeholders"

# Self-contained Gradle wrapper.
for f in gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties settings.gradle; do
  [[ -f "$ROOT/$f" ]] || fail "missing $f"
done
[[ -x "$ROOT/gradlew" ]] || fail "gradlew not executable"
pass "gradle wrapper complete"

echo "ALL CHECKS PASSED"
