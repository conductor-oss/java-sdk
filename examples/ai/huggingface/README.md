# Hugging Face Inference in Java Using Conductor : Task-Based Model Selection and API Orchestration

## Multi-Task NLP with a Single Workflow

Different NLP tasks require different models with different input formats and output structures. Summarization uses BART and returns a summary string. Text generation uses GPT-2 and returns a continuation. Sentiment analysis uses a classifier and returns labels with scores. Each model has its own API parameters (`min_length` for summarization, `max_new_tokens` for generation) and its own output format.

When you hardcode a single model call, adding a new task type means duplicating the entire pipeline. When you handle model selection and inference in the same method, a Hugging Face API timeout means re-running the model selection logic. And without tracking which model was used for which request, you can't debug why a particular summarization was poor.

## The Solution

**You write the model selection, inference call, and result formatting logic. Conductor handles the task routing, retries, and observability.**

Each concern is an independent worker. model selection (mapping task types to Hugging Face model IDs and parameters), inference (calling the Hugging Face Inference API), and result formatting (extracting the relevant field from the model's output based on task type). Conductor chains them so the selected model feeds into the inference call, and the raw output feeds into the task-specific formatter. If the Inference API is rate-limited or a model is loading, Conductor retries automatically. Every execution records which model handled which task.

### What You Write: Workers

Three workers cover task-based model routing. selecting the right Hugging Face model for the task (summarization, generation, or sentiment), running inference, and formatting the task-specific result.

| Worker | Task | What It Does |
|---|---|---|
| **HfFormatResultWorker** | `hf_format_result` | Formats the raw inference output based on the task type. Extracts the relevant field from the model output (e.g, summ... |
| **HfInferenceWorker** | `hf_inference` | Simulates a call to the Hugging Face Inference API. In production, this would POST to https://api-inference.huggingfa... |
| **HfSelectModelWorker** | `hf_select_model` | Selects the appropriate Hugging Face model based on the requested task type. Maps task types (summarization, text-gen... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
hf_select_model
 │
 ▼
hf_inference
 │
 ▼
hf_format_result

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
