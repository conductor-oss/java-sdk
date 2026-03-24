# Voice Bot: Transcribe, Understand, Generate, Synthesize

A caller says "Where is my order 1234?" The system must transcribe the audio, understand the intent and extract entities (like `orderNumber: "1234"`), generate a spoken response, and synthesize it back to audio for playback.

## Workflow

```
audioUrl, callerId, language
       │
       ▼
┌──────────────────┐
│ vb_transcribe    │  Speech-to-text
└────────┬─────────┘
         ▼
┌──────────────────┐
│ vb_understand    │  NLU: intent + entity extraction
└────────┬─────────┘
         ▼
┌──────────────────┐
│ vb_generate      │  Generate spoken response
└────────┬─────────┘
         ▼
┌──────────────────┐
│ vb_synthesize    │  Text-to-speech
└──────────────────┘
```

## Workers

**TranscribeWorker** (`vb_transcribe`) -- Converts the audio URL to text (speech-to-text).

**UnderstandWorker** (`vb_understand`) -- Extracts intent and entities from the transcribed text. Returns `entities: {orderNumber: "1234"}`.

**GenerateWorker** (`vb_generate`) -- Takes the extracted entities (defaulting to `Map.of()` if missing) and generates a spoken response appropriate for voice delivery.

**SynthesizeWorker** (`vb_synthesize`) -- Converts the response text to audio. Returns an audio URL: `"https://tts.example.com/audio/" + System.currentTimeMillis() + ".mp3"`.

## Tests

8 tests cover transcription, NLU understanding, response generation, and audio synthesis.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
