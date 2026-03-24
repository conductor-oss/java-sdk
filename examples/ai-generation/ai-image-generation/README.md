# Image Generation: Prompt Engineering, Generate, Enhance, Validate, Deliver

An image prompt is refined, the image is generated (calling `https://api.openai.com/v1/images/generations` when API key is set, or returning `"https://example.com/deterministic.image.png"` in fallback), enhanced, validated for quality, and delivered.

## Workflow

```
prompt, style, resolution
  -> aig_prompt -> aig_generate -> aig_enhance -> aig_validate -> aig_deliver
```

## Tests

2 tests cover the image generation pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
