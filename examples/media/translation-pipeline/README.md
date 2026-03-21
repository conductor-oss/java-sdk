# Translation Pipeline in Java Using Conductor :  Language Detection, Machine Translation, Human Review, and Locale Publishing

A Java Conductor workflow example that orchestrates a content translation pipeline. detecting the source language with confidence scores and alternative language candidates, performing machine translation with quality scoring, routing through human review for corrections and quality assurance, and publishing the approved translation to locale-specific URLs. Uses [Conductor](https://github.

## Why Translation Pipelines Need Orchestration

Translating content for international audiences involves a pipeline where quality gates prevent bad translations from going live. You detect the source language. sometimes user-submitted content is mislabeled or contains mixed languages, so automated detection with confidence scoring prevents translation from the wrong source. You run machine translation to produce a draft with word counts and quality scores. A human reviewer checks the machine output,  correcting errors, improving fluency, and assigning a review score. Only after human approval does the translation get published to its locale-specific URL.

Each stage depends on the previous one. machine translation needs the correct source language, human review needs the machine output, and publishing needs the reviewed text. If language detection is uncertain, the translation might be garbage. If you skip human review, brand-damaging errors reach your international audience. Without orchestration, you'd build a monolithic translation system that mixes language detection APIs, translation APIs, reviewer assignment, and CMS publishing,  making it impossible to swap translation providers, route different content types through different review processes, or trace which translation quality issues came from the machine vs, the reviewer.

## How This Workflow Solves It

**You just write the translation workers. Language detection, machine translation, human review, and locale publishing. Conductor handles quality-gated sequencing, translation API retries, and reviewer correction records for quality improvement.**

Each translation stage is an independent worker. detect language, translate, human review, publish. Conductor sequences them, passes source text and translated drafts between stages, retries if a translation API times out, and records every language detection, translation quality score, and reviewer correction for quality analysis.

### What You Write: Workers

Four workers handle the translation flow: DetectLanguageWorker identifies the source language with confidence scoring, TranslateWorker produces machine drafts with quality metrics, ReviewTranslationWorker captures human corrections, and PublishTranslationWorker pushes to locale-specific URLs.

| Worker | Task | What It Does |
|---|---|---|
| **DetectLanguageWorker** | `trn_detect_language` | Detects the language |
| **PublishTranslationWorker** | `trn_publish_translation` | Publishes the translation |
| **ReviewTranslationWorker** | `trn_review_translation` | Reviews the translation |
| **TranslateWorker** | `trn_translate` | Translates the content and computes translated text, quality score, word count, model version |

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction,  with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

### The Workflow

```
trn_detect_language
    │
    ▼
trn_translate
    │
    ▼
trn_review_translation
    │
    ▼
trn_publish_translation

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
java -jar target/translation-pipeline-1.0.0.jar

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
java -jar target/translation-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow translation_pipeline_workflow \
  --version 1 \
  --input '{"contentId": "TEST-001", "sourceText": "Process this order for customer C-100", "targetLanguage": "production"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w translation_pipeline_workflow -s COMPLETED -c 5

```

## How to Extend

Connect DetectLanguageWorker to a language detection API, TranslateWorker to your translation service (Google Translate, DeepL), and ReviewTranslationWorker to your linguist review queue. The workflow definition stays exactly the same.

- **DetectLanguageWorker** (`trn_detect_language`): call a language detection API (Google Cloud Translation, AWS Comprehend, fastText) to identify the source language with confidence scores and alternative candidates
- **TranslateWorker** (`trn_translate`): perform machine translation using Google Translate API, DeepL, or AWS Translate, returning the translated text with quality scores and model version tracking
- **ReviewTranslationWorker** (`trn_review_translation`): route to human translators via a translation management system (Smartling, Crowdin, Transifex) for corrections, fluency improvements, and quality scoring
- **PublishTranslationWorker** (`trn_publish_translation`): publish the approved translation to locale-specific URLs in your CMS, update hreflang tags, and configure locale routing

Swap any worker for a different translation provider or CMS integration while keeping the same return structure, and the pipeline adapts without modification.

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
translation-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/translationpipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TranslationPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DetectLanguageWorker.java
│       ├── PublishTranslationWorker.java
│       ├── ReviewTranslationWorker.java
│       └── TranslateWorker.java
└── src/test/java/translationpipeline/workers/
    ├── DetectLanguageWorkerTest.java        # 2 tests
    └── TranslateWorkerTest.java        # 2 tests

```
