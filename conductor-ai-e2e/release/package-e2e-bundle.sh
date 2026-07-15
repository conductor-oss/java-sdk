#!/usr/bin/env bash
set -euo pipefail

# ── Package the agent e2e suite as a standalone bundle ───────────────────────
# Builds conductor-ai-e2e-java-<version>.tar.gz: a self-contained Gradle
# project carrying the conductor-ai-e2e test sources, pinned to the published
# org.conductoross:conductor-ai:<version> artifact (no SDK source vendored).
#
# Downstream repos (e.g. orkes-io/orkes-conductor) download the bundle from
# the java-sdk GitHub release and run it against their own server build. This
# replaces the agentspan-sdk-e2e-java-* bundles formerly cut from
# agentspan-ai/agentspan — java-sdk is now the canonical home of these suites.
#
# Usage:
#   ./conductor-ai-e2e/release/package-e2e-bundle.sh --version 5.1.0 [--out DIR]
#
# The bundle only references the SDK by Maven coordinate, so packaging needs
# no compilation and no network — the pinned version does not have to be on
# Maven Central yet (release ordering: this can run before the publish job
# finishes staging).

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"

VERSION=""
OUT_DIR="$HERE/dist"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) VERSION="$2"; shift 2 ;;
    --out)     OUT_DIR="$2"; shift 2 ;;
    *) echo "ERROR: unknown arg '$1' (want --version X.Y.Z [--out DIR])" >&2; exit 1 ;;
  esac
done

[[ -n "$VERSION" ]] || { echo "ERROR: --version is required" >&2; exit 1; }

NAME="conductor-ai-e2e-java-$VERSION"
STAGE="$OUT_DIR/$NAME"

echo "Packaging agent e2e bundle ($NAME)..."
rm -rf "$STAGE"
mkdir -p "$STAGE/src/test/java"

# The e2e sources are in the default package (no package decl), so they live
# directly under src/test/java in the standalone Gradle layout.
cp "$REPO_ROOT"/conductor-ai-e2e/src/test/java/*.java "$STAGE/src/test/java/"

# Standalone build pins the published SDK; framework/test deps mirror
# conductor-ai-e2e/build.gradle (versions from versions.gradle) so the
# framework-bridge suites compile + link.
cat > "$STAGE/build.gradle" <<'EOF'
plugins {
    id 'java'
}

group = 'org.conductoross'

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

// Pinned to the java-sdk release this bundle was cut from. Override to test
// an unreleased SDK:  ./gradlew test -PconductorAiVersion=X.Y.Z-SNAPSHOT -PuseMavenLocal
def conductorAiVersion = project.findProperty('conductorAiVersion') ?: '@VERSION@'

repositories {
    if (project.hasProperty('useMavenLocal')) {
        mavenLocal()
    }
    mavenCentral()
}

ext {
    junitVersion         = '5.10.3'
    junitPlatformVersion = '1.10.3'   // launcher must match the Jupiter 5.x platform
    langchain4jVersion   = '1.0.0'
    googleAdkVersion     = '1.3.0'
    langgraph4jVersion   = '1.6.0-beta5'
}

dependencies {
    testImplementation "org.conductoross:conductor-ai:${conductorAiVersion}"

    testImplementation "org.junit.jupiter:junit-jupiter-api:${junitVersion}"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"
    // Gradle 9 no longer puts the JUnit Platform launcher on the test runtime
    // classpath automatically (Gradle 8 did). Declare it so the bundle runs on
    // both Gradle 8 and 9.
    testRuntimeOnly "org.junit.platform:junit-platform-launcher:${junitPlatformVersion}"
    testImplementation 'ch.qos.logback:logback-classic:1.5.32'

    // LLM frameworks: not imported by the suites directly, but the SDK's bridge
    // classes (compileOnly there) need them on the runtime classpath when the
    // framework-facing suites execute.
    testImplementation "dev.langchain4j:langchain4j:${langchain4jVersion}"
    testImplementation "dev.langchain4j:langchain4j-open-ai:${langchain4jVersion}"
    testImplementation "com.google.adk:google-adk:${googleAdkVersion}"
    testImplementation "org.bsc.langgraph4j:langgraph4j-core:${langgraph4jVersion}"
    testImplementation "org.bsc.langgraph4j:langgraph4j-agent-executor:${langgraph4jVersion}"
}

// tool/agent parameter names are read reflectively at runtime
compileTestJava.options.compilerArgs << '-parameters'

test {
    useJUnitPlatform()
    testLogging {
        events 'passed', 'skipped', 'failed'
        exceptionFormat 'full'
    }
    // e2e suites are I/O-bound (LLM calls) and use unique agent/task names,
    // so they can safely run concurrently.
    maxParallelForks = 3
    // BaseTest reads these from the environment; a -D on the gradle command
    // line wins over the caller's env, and the defaults apply when neither
    // is set.
    environment 'AGENTSPAN_SERVER_URL',
        System.getProperty('AGENTSPAN_SERVER_URL', System.getenv('AGENTSPAN_SERVER_URL') ?: 'http://localhost:8080/api')
    environment 'AGENTSPAN_LLM_MODEL',
        System.getProperty('AGENTSPAN_LLM_MODEL', System.getenv('AGENTSPAN_LLM_MODEL') ?: 'openai/gpt-4o-mini')
}
EOF
sed -i.bak "s/@VERSION@/$VERSION/g" "$STAGE/build.gradle" && rm "$STAGE/build.gradle.bak"

cat > "$STAGE/settings.gradle" <<'EOF'
rootProject.name = 'conductor-ai-e2e-java'
EOF

# Bundle the repo's pinned Gradle wrapper so the suite is self-contained and
# runs identically regardless of the host's Gradle — no system gradle needed.
cp "$REPO_ROOT/gradlew"     "$STAGE/gradlew"
cp "$REPO_ROOT/gradlew.bat" "$STAGE/gradlew.bat"
mkdir -p "$STAGE/gradle/wrapper"
cp "$REPO_ROOT/gradle/wrapper/gradle-wrapper.jar"        "$STAGE/gradle/wrapper/"
cp "$REPO_ROOT/gradle/wrapper/gradle-wrapper.properties" "$STAGE/gradle/wrapper/"
chmod +x "$STAGE/gradlew"

cat > "$STAGE/run.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
# Runs the agent e2e suite against a live Conductor server with the agent
# runtime enabled (conductor-oss >= 3.32.0-rc.8, or orkes-conductor with
# agentspan.embedded=true).
#
# Required services (NOT started by this script):
#   - Conductor server   → AGENTSPAN_SERVER_URL (default http://localhost:8080/api)
#   - MCP testkit on http://localhost:9999/mcp (Suite4McpTools; URL is fixed
#     in the suite)
# Optional:
#   - AGENTSPAN_LLM_MODEL (default openai/gpt-4o-mini); the matching provider
#     API key must be configured on the SERVER — the suites never read it
#     (asserted by Suite2ToolCallingCredentials).
#
# Requires only JDK 21 — the bundled Gradle wrapper (./gradlew) pins the Gradle
# version, so no system gradle is needed. Usage: ./run.sh [extra gradle args]
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"
./gradlew test \
  -DAGENTSPAN_SERVER_URL="${AGENTSPAN_SERVER_URL:-http://localhost:8080/api}" \
  -DAGENTSPAN_LLM_MODEL="${AGENTSPAN_LLM_MODEL:-openai/gpt-4o-mini}" "$@"
echo "Report: $HERE/build/reports/tests/test/index.html"
EOF
chmod +x "$STAGE/run.sh"

cat > "$STAGE/README.md" <<'EOF'
# Conductor Agent SDK (java) — E2E suite @VERSION@

Self-contained end-to-end tests for the Conductor Java agent SDK, pinned to
release **@VERSION@**. Resolves `org.conductoross:conductor-ai:@VERSION@` from
Maven Central — no SDK source is vendored. Cut from
[conductor-oss/java-sdk](https://github.com/conductor-oss/java-sdk)
(`conductor-ai-e2e/`); supersedes the `agentspan-sdk-e2e-java-*` bundles
formerly released from agentspan-ai/agentspan.

## Prerequisites (you provide these)

| Requirement                     | Env var                | Default                     |
|---------------------------------|------------------------|-----------------------------|
| JDK 21 (Gradle wrapper bundled) | —                      | —                           |
| Conductor server w/ agent runtime | `AGENTSPAN_SERVER_URL` | `http://localhost:8080/api` |
| LLM model                       | `AGENTSPAN_LLM_MODEL`  | `openai/gpt-4o-mini`        |
| MCP testkit (Suite4 only)       | — (fixed in suite)     | `http://localhost:9999/mcp` |

The server needs the agent runtime: conductor-oss `>= 3.32.0-rc.8`, or
orkes-conductor booted with `agentspan.embedded=true`. LLM provider API keys
(e.g. `OPENAI_API_KEY`) go to the **server** process, not this suite — the
suites intentionally never read them (`Suite2ToolCallingCredentials` asserts
this; credentials reach workers via the `runtimeMetadata` wire contract).

## Run

```bash
./run.sh                       # full suite
./run.sh --tests 'Suite1*'     # filter, plus any gradle args
```

JUnit XML lands in `build/test-results/test/`, HTML report in
`build/reports/tests/test/`.

## Testing an unreleased SDK

```bash
./gradlew test -PconductorAiVersion=X.Y.Z-SNAPSHOT -PuseMavenLocal
```
EOF
sed -i.bak "s/@VERSION@/$VERSION/g" "$STAGE/README.md" && rm "$STAGE/README.md.bak"

# Tarball extracts to conductor-ai-e2e-java-<version>/ ; stage dir is removed
# so dist/ holds only the artifacts to upload.
mkdir -p "$OUT_DIR"
tar -czf "$OUT_DIR/$NAME.tar.gz" -C "$OUT_DIR" "$NAME"
rm -rf "$STAGE"

echo "OK: $OUT_DIR/$NAME.tar.gz"
