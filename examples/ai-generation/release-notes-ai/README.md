# Release Notes AI in Java with Conductor :  Generate Release Notes from Git Commits Between Tags

A Java Conductor workflow that generates release notes automatically .  collecting commits between two git tags, categorizing them by type (feature, bugfix, improvement, breaking change), generating human-readable release notes from the categorized commits, and publishing the notes. Given a `repoName`, `fromTag`, and `toTag`, the pipeline produces commit lists, categorized changes, formatted release notes, and a publish URL. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the collect-categorize-generate-publish pipeline.

## Writing Release Notes That No One Wants to Write

Release notes are essential for users but tedious for developers. Manually scanning git log output, deciding which commits matter, grouping them into categories, and writing user-facing descriptions takes time that developers would rather spend coding. The information is already in the commit history .  it just needs to be extracted, organized, and rewritten for a non-developer audience.

This workflow automates release note generation. The commit collector gathers all commits between the `fromTag` and `toTag`. The categorizer labels each commit as a feature, bugfix, improvement, or breaking change. The notes generator transforms the categorized commits into polished, user-facing release notes grouped by category. The publisher posts the generated notes to the appropriate platform. Each step's output feeds the next .  commits feed categorization, categories feed note generation, and generated notes feed publishing.

## The Solution

**You just write the commit-collection, categorization, notes-generation, and publishing workers. Conductor handles the release-notes pipeline and tag-range data flow.**

Four workers handle the release notes pipeline .  commit collection, categorization, note generation, and publishing. The collector pulls commits between tags. The categorizer labels each commit by type. The generator produces human-readable notes grouped by category. The publisher posts the final document. Conductor sequences the four steps and passes commit lists, categories, and formatted notes between them via JSONPath.

### What You Write: Workers

CollectCommitsWorker gathers commits between tags, CategorizeWorker labels each as feature/bugfix/improvement, GenerateNotesWorker transforms them into user-facing copy, and PublishWorker posts the final document.

| Worker | Task | What It Does |
|---|---|---|
| **CategorizeWorker** | `rna_categorize` | Labels each commit as a feature, bugfix, improvement, or breaking change. |
| **CollectCommitsWorker** | `rna_collect_commits` | Gathers all commits between the fromTag and toTag from the repository. |
| **GenerateNotesWorker** | `rna_generate_notes` | Transforms categorized commits into polished, user-facing release notes grouped by category. |
| **PublishWorker** | `rna_publish` | Publishes the generated release notes to a configured platform. |

Workers implement domain operations .  lead scoring, contact enrichment, deal updates ,  with realistic outputs. Replace with real CRM API integrations and the workflow stays the same.

### The Workflow

```
rna_collect_commits
    │
    ▼
rna_categorize
    │
    ▼
rna_generate_notes
    │
    ▼
rna_publish
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/release-notes-ai-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/release-notes-ai-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow rna_release_notes \
  --version 1 \
  --input '{"repoName": "test", "fromTag": "test-value", "toTag": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w rna_release_notes -s COMPLETED -c 5
```

## How to Extend

Each worker handles one release-notes step .  connect your Git API (GitHub, GitLab) for commit collection and your publishing platform (GitHub Releases, Notion, Confluence) for output, and the release-notes workflow stays the same.

- **CategorizeWorker** (`rna_categorize`): use Conventional Commits parsing or an LLM for more accurate commit classification
- **CollectCommitsWorker** (`rna_collect_commits`): integrate with the GitHub/GitLab API to pull real commits between release tags
- **GenerateNotesWorker** (`rna_generate_notes`): swap in an LLM (GPT-4, Claude) to generate polished, user-facing release notes

Integrate the GitHub API for real commit collection and the release-notes generation pipeline stays intact.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
release-notes-ai/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/releasenotesai/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── ReleaseNotesAiExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CategorizeWorker.java
│       ├── CollectCommitsWorker.java
│       ├── GenerateNotesWorker.java
│       └── PublishWorker.java
└── src/test/java/releasenotesai/workers/
    ├── CategorizeWorkerTest.java        # 2 tests
    ├── CollectCommitsWorkerTest.java        # 2 tests
    ├── GenerateNotesWorkerTest.java        # 2 tests
    └── PublishWorkerTest.java        # 2 tests
```
