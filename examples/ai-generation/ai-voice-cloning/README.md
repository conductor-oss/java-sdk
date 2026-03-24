# Voice Cloning: Collect Samples, Train Model, Generate Speech, Verify Quality

Clone a voice: collect audio samples from the speaker, train a voice model, generate speech for the target text, verify quality (similarity and naturalness scores 0.0-1.0 via LLM), and deliver the audio.

## Workflow

```
speakerId, targetText, language
  -> avc_collect_samples -> avc_train_model -> avc_generate -> avc_verify -> avc_deliver
```

## Tests

2 tests cover the voice cloning pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
