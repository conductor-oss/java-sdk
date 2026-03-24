# Translation Pipeline

Orchestrates translation pipeline through a multi-stage Conductor workflow.

**Input:** `contentId`, `sourceText`, `targetLanguage` | **Timeout:** 60s

## Pipeline

```
trn_detect_language
    │
trn_translate
    │
trn_review_translation
    │
trn_publish_translation
```

## Workers

**DetectLanguageWorker** (`trn_detect_language`)

Reads `detectedLanguage`. Outputs `detectedLanguage`, `confidence`, `alternativeLanguages`, `score`.

**PublishTranslationWorker** (`trn_publish_translation`)

Reads `translationUrl`. Outputs `translationUrl`, `publishedAt`, `locale`.

**ReviewTranslationWorker** (`trn_review_translation`)

Reads `approved`, `translatedText`. Outputs `approved`, `finalText`, `reviewerId`, `corrections`, `reviewScore`.

**TranslateWorker** (`trn_translate`)

Reads `translatedText`. Outputs `translatedText`, `qualityScore`, `wordCount`, `modelVersion`.

## Tests

**4 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
