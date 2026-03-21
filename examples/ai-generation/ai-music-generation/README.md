# AI Music Generation in Java Using Conductor : Compose, Arrange, Produce, Master, Deliver

## Music Production Has Distinct Creative Stages

Generating a 30-second lofi hip-hop track in a relaxed mood requires five professional production stages: composition (melody, chord progression, rhythm pattern in the right key and tempo), arrangement (selecting instruments. piano, bass, drums, synth pads, and assigning parts), production (applying effects, reverb, compression, EQ, and mixing levels), mastering (final loudness normalization, stereo width, format compliance), and delivery (encoding to the target format with metadata).

Each stage transforms the music: composition creates the musical ideas, arrangement gives them sonic identity, production shapes the sound, and mastering ensures it sounds good on any playback system. If the arrangement step produces an instrument combination that sounds muddy, you can retry it without recomposing the melody.

## The Solution

**You just write the composition, arrangement, production, mastering, and delivery logic. Conductor handles retry logic, stage ordering, and production audit trails from composition to final master.**

`ComposeWorker` generates the core musical elements. melody, chord progression, rhythm, key, tempo, and structure, matching the requested genre and mood. `ArrangeWorker` selects instruments and assigns parts to create the full arrangement. `ProduceWorker` applies effects (reverb, compression, EQ), mixes levels, and adds production polish. `MasterWorker` normalizes loudness, adjusts stereo width, and ensures format compliance. `DeliverWorker` encodes the final audio with metadata (genre, BPM, key, duration) and delivers the file. Conductor records the full production chain for creative iteration.

### What You Write: Workers

From composition through mastering, each worker in this pipeline owns a single phase of AI music production and passes structured output to the next.

| Worker | Task | What It Does |
|---|---|---|
| **ArrangeWorker** | `amg_arrange` | Arranges the composition for piano, strings, and drums. selects instruments and assigns parts for the target duration |
| **ComposeWorker** | `amg_compose` | Composes the core musical elements. melody, chord progression, rhythm, key, and tempo matching the requested genre and mood |
| **DeliverWorker** | `amg_deliver` | Delivers the mastered track as WAV format to the output destination with file size metadata |
| **MasterWorker** | `amg_master` | Masters the produced track. normalizes loudness to -14 LUFS and applies final polish for distribution |
| **ProduceWorker** | `amg_produce` | Produces the track. applies mixing, effects (reverb, compression, EQ), and balances audio levels at 48kHz/24-bit |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
amg_compose
 │
 ▼
amg_arrange
 │
 ▼
amg_produce
 │
 ▼
amg_master
 │
 ▼
amg_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
