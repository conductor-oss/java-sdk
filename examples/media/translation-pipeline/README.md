# Translation Pipeline in Java Using Conductor : Language Detection, Machine Translation, Human Review, and Locale Publishing

## Why Translation Pipelines Need Orchestration

Translating content for international audiences involves a pipeline where quality gates prevent bad translations from going live. You detect the source language. sometimes user-submitted content is mislabeled or contains mixed languages, so automated detection with confidence scoring prevents translation from the wrong source. You run machine translation to produce a draft with word counts and quality scores. A human reviewer checks the machine output, correcting errors, improving fluency, and assigning a review score. Only after human approval does the translation get published to its locale-specific URL.

Each stage depends on the previous one. machine translation needs the correct source language, human review needs the machine output, and publishing needs the reviewed text. If language detection is uncertain, the translation might be garbage. If you skip human review, brand-damaging errors reach your international audience. Without orchestration, you'd build a monolithic translation system that mixes language detection APIs, translation APIs, reviewer assignment, and CMS publishing, making it impossible to swap translation providers, route different content types through different review processes, or trace which translation quality issues came from the machine vs, the reviewer.

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

Workers implement media processing stages. transcoding, thumbnail generation, metadata extraction, with realistic output artifacts. Replace with real media tools (FFmpeg, ImageMagick) and the pipeline stays the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
