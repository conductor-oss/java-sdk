# Audio Transcription

A media company receives hundreds of audio files daily from podcast hosts, customer support calls, and voicemail systems. Each file needs language detection, transcription, and keyword extraction before it can be indexed. If the transcription service is temporarily unavailable, the entire batch stalls and files pile up in the ingest queue.

## Pipeline

```
[au_preprocess_audio]
     |
     v
[au_transcribe]
     |
     v
[au_generate_timestamps]
     |
     v
[au_extract_keywords]
```

**Workflow inputs:** `audioUrl`, `language`, `speakerCount`

## Workers

**ExtractKeywordsWorker** (task: `au_extract_keywords`)

Extracts keywords from the transcript.

- `DEFAULT_KEYWORDS` = List.of("machine learning", "infrastructure", "scalability",
            "training data", "distributed processing", "real-time validation")
- Formats output strings
- Writes `keywords`, `keywordCount`

**GenerateTimestampsWorker** (task: `au_generate_timestamps`)

Generates timestamps for transcript segments.

- Formats output strings
- Reads `segments`. Writes `timestamped`, `segmentCount`

**PreprocessAudioWorker** (task: `au_preprocess_audio`)

Preprocesses audio: normalization, noise reduction.

- Writes `processedAudio`, `duration`, `sampleRate`, `channels`

**TranscribeWorker** (task: `au_transcribe`)

Transcribes audio into text with speaker diarization.

- Writes `transcript`, `segments`, `wordCount`, `speakersDetected`

---

**24 tests** | Workflow: `audio_transcription` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
