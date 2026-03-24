# AI Voice Cloning: Sample Collection Through Verified Delivery

A company needs to produce audio content in a specific speaker's voice for audiobooks, voiceovers, or accessibility features, but the speaker is unavailable for every recording session. Voice cloning solves this by training a model on existing samples, then generating new speech in the cloned voice. The critical requirement is verification -- the generated audio must closely match the original speaker's characteristics before delivery.

This workflow collects voice samples, trains a cloning model, generates speech, verifies quality, and delivers the audio.

## Pipeline Architecture

```
speakerId, targetText, language
         |
         v
  avc_collect_samples    (sampleCount=25, totalDuration="12 minutes")
         |
         v
  avc_train_model        (modelId, epochs=500, loss=0.023)
         |
         v
  avc_generate           (audioId, duration=8.5 seconds)
         |
         v
  avc_verify             (similarity=0.96, naturalness=0.91, verified=true)
         |
         v
  avc_deliver            (delivered=true, url as MP3 path)
```

## Worker: CollectSamples (`avc_collect_samples`)

Gathers audio samples for the given `speakerId` and `language`. Returns `sampleCount: 25` audio clips with a `totalDuration: "12 minutes"` of recorded speech. The sample count and duration determine the quality ceiling for the trained model.

## Worker: TrainModel (`avc_train_model`)

Trains a voice cloning model on the collected samples. Returns `modelId: "VMDL-ai-voice-cloning-001"` (prefixed with `VMDL` for voice model), `epochs: 500` training iterations, and a final `loss: 0.023`. The low loss value indicates the model has converged and closely reproduces the speaker's vocal characteristics.

## Worker: Generate (`avc_generate`)

Uses the trained model to synthesize speech for the `targetText`. Returns `audioId: "AUD-ai-voice-cloning-001"` and `duration: 8.5` seconds. The duration depends on the length of the target text and the speaking rate of the cloned voice.

## Worker: Verify (`avc_verify`)

Compares the generated audio against the original speaker samples. When an LLM is available, generates detailed `verificationDetails` via the model. Returns `similarity: 0.96` (how closely the voice matches the original speaker on a 0-1 scale), `naturalness: 0.91` (how human-like the speech sounds), and `verified: true` indicating the output meets quality thresholds.

## Worker: Deliver (`avc_deliver`)

Delivers the verified audio file in MP3 format. Returns `delivered: true` and `url: "/audio/AUD-ai-voice-cloning-001.mp3"`.

## Tests

2 tests cover the voice cloning pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
