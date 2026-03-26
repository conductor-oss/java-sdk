# Multi-Task NLP via Hugging Face: Model Selection by Task Type

Different NLP tasks need different models with different output formats. Summarization uses BART and returns `summary_text`. Text generation uses Mixtral and returns `generated_text`. Sentiment analysis uses DistilBERT and returns `label`. This workflow routes to the right model based on the task type, calls the Hugging Face Inference API, and parses the task-specific output format.

## Workflow

```
text, task
   │
   ▼
┌──────────────────┐
│ hf_select_model  │  Map task type to model ID + params
└────────┬─────────┘
         │  modelId, formattedInput, parameters, options
         ▼
┌──────────────────┐
│ hf_inference     │  POST to api-inference.huggingface.co
└────────┬─────────┘
         │  rawOutput
         ▼
┌──────────────────┐
│ hf_format_result │  Extract task-specific field
└──────────────────┘
         │
         ▼
   result, model, task
```

## Workers

**HfSelectModelWorker** (`hf_select_model`) -- Maps task types to model IDs via a static `TASK_MODEL_MAP`: `"text-generation"` -> `"mistralai/Mixtral-8x7B-Instruct-v0.1"`, `"summarization"` -> `"facebook/bart-large-cnn"`, `"sentiment-analysis"` -> `"distilbert-base-uncased-finetuned-sst-2-english"`, `"translation"` -> `"Helsinki-NLP/opus-mt-en-fr"`. Unknown task types default to text-generation. Returns inference parameters: `max_new_tokens: 250`, `temperature: 0.6`, `top_p: 0.9`, `repetition_penalty: 1.1`, `do_sample: true`. Also sets `options: {wait_for_model: true, use_cache: false}`.

**HfInferenceWorker** (`hf_inference`) -- Checks `HUGGINGFACE_TOKEN` at construction. In live mode, POSTs `{"inputs": text}` to `https://api-inference.huggingface.co/models/<modelId>` with a 10-second connect timeout and 120-second request timeout, `Authorization: Bearer <token>`. In fallback mode, returns a hardcoded summarization result about a hybrid work study (2,500 workers, 12 countries, 18%/23% productivity improvements).

**HfFormatResultWorker** (`hf_format_result`) -- Extracts the result based on task type: `"summarization"` reads `output.get(0).get("summary_text")`, `"sentiment-analysis"` reads `.get("label")`, everything else reads `.get("generated_text")` with a `.toString()` fallback. Logs the first 60 characters of the result.

## Tests

17 tests cover model selection for all task types, inference in both modes, and format extraction for each output shape.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
