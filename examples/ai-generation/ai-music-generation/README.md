# AI Music Generation: Compose, Arrange, Produce, Master, Deliver

A content creator needs background music in a specific genre and mood for a video project. Producing music manually requires composition expertise, knowledge of arrangement and instrumentation, audio production skills, and mastering tools. Automating this pipeline lets non-musicians generate production-quality tracks by specifying just genre, mood, and duration.

This workflow models the five stages of professional music production: composition, arrangement, production, mastering, and delivery.

## Pipeline Architecture

```
genre, mood, durationSec
       |
       v
amg_compose              (composition="C-major-120bpm", key, tempo, bars=32)
       |
       v
amg_arrange              (arrangement, actualDuration=180, instruments=3)
       |
       v
amg_produce              (trackId, sampleRate=48000, bitDepth=24)
       |
       v
amg_master               (masteredTrackId, loudness=-14 LUFS)
       |
       v
amg_deliver              (delivered=true, url as WAV path, sizeMB=45)
```

## Worker: Compose (`amg_compose`)

Generates a musical composition based on the input `genre` and `mood`. Returns `composition: "C-major-120bpm"`, `key: "C major"`, `tempo: 120` BPM, and `bars: 32`. The composition identifier encodes the key and tempo for downstream processing.

## Worker: Arrange (`amg_arrange`)

Takes the composition and target `durationSec` and arranges it for multiple instruments. Returns `arrangement: "arranged-3-instruments"`, `actualDuration: 180` seconds (3 minutes), and `instruments: 3`. The actual duration may differ from the requested duration based on the arrangement structure.

## Worker: Produce (`amg_produce`)

Produces the arranged track into a digital audio file. Returns `trackId: "TRK-ai-music-generation-001"`, `sampleRate: 48000` Hz (professional quality), and `bitDepth: 24` bits. These specifications match broadcast and streaming standards.

## Worker: Master (`amg_master`)

Masters the produced track by normalizing loudness and applying final processing. Returns `masteredTrackId: "TRK-ai-music-generation-001-M"` (appending `-M` to denote mastered) and `loudness: -14` LUFS (matching the standard loudness target for streaming platforms like Spotify and YouTube).

## Worker: Deliver (`amg_deliver`)

Delivers the mastered track in WAV format. Returns `delivered: true`, `url: "/tracks/TRK-ai-music-generation-001-M.wav"`, and `sizeMB: 45`. The file size reflects the uncompressed WAV format at 48kHz/24-bit for a 3-minute track.

## Tests

2 tests cover the music generation pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
