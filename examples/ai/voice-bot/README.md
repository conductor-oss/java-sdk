# Voice Bot in Java with Conductor

## Handling Voice Conversations End-to-End

A voice bot needs to process spoken language through four distinct stages: convert audio to text, understand what the caller wants, generate an appropriate response, and convert the response back to speech. Each stage depends on the previous one. you cannot understand intent without a transcript, and you cannot synthesize speech without a response to speak. Failures at any stage (bad audio, ambiguous intent, synthesis errors) need proper handling and retry logic.

This workflow processes a single voice interaction. The transcriber converts the caller's audio to text. The intent analyzer identifies what the caller wants (e.g., order status, account inquiry) and extracts relevant entities (order numbers, account IDs). The response generator produces a contextual reply based on the detected intent. The synthesizer converts the text response into an audio file that can be played back to the caller.

## The Solution

**You just write the transcription, intent-analysis, response-generation, and speech-synthesis workers. Conductor handles the voice pipeline sequencing.**

Each worker handles one CRM operation. Conductor manages the customer lifecycle pipeline, assignment routing, follow-up scheduling, and activity tracking.

### What You Write: Workers

TranscribeWorker converts audio to text, UnderstandWorker detects intent and extracts entities like order numbers, GenerateWorker crafts a contextual reply, and SynthesizeWorker converts it back to speech.

| Worker | Task | What It Does |
|---|---|---|
| **GenerateWorker** | `vb_generate` | Produces a contextual text response based on the detected intent and extracted entities. |
| **SynthesizeWorker** | `vb_synthesize` | Converts the text response into a speech audio file for playback to the caller. |
| **TranscribeWorker** | `vb_transcribe` | Converts caller audio into text using speech-to-text processing. |
| **UnderstandWorker** | `vb_understand` | Detects the caller's intent (e.g., order_status) and extracts entities (e.g., order numbers). |

### The Workflow

```
vb_transcribe
 │
 ▼
vb_understand
 │
 ▼
vb_generate
 │
 ▼
vb_synthesize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
