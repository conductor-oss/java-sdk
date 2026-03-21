# AI Image Generation in Java Using Conductor : Prompt Engineering, Generation, Enhancement, Validation, Delivery

## From Text Prompt to Production-Ready Image

A user requests "a photorealistic mountain landscape at sunset, 4K resolution." The raw prompt needs enrichment with model-specific keywords (lighting terms, style modifiers, negative prompts) before the diffusion model produces good results. The generated image may need upscaling to the requested resolution. Before delivery, the image must pass content safety checks (no unintended artifacts, NSFW detection) and specification validation (correct resolution, aspect ratio, style match).

Each step has different failure modes. prompt engineering might produce conflicting style directives, the model might timeout on high-resolution generation, the enhancer might introduce artifacts, and the validator might reject the image. Without orchestration, a failed generation means re-engineering the prompt, and there's no record of which prompt version produced which image.

## The Solution

**You just write the prompt engineering, image generation, enhancement, content validation, and delivery logic. Conductor handles retries, step sequencing, and full audit trails for every generated image.**

`PromptWorker` transforms the user's description into an optimized prompt with model-specific keywords, style modifiers, negative prompts, and technical parameters. `GenerateWorker` sends the engineered prompt to the image generation model and returns the raw output. `EnhanceWorker` upscales the image to the target resolution and applies quality improvements. `ValidateWorker` checks content safety, resolution compliance, and style adherence. `DeliverWorker` packages the validated image with metadata (prompt used, model version, generation parameters) and delivers it. Conductor records the full prompt-to-image chain for reproducibility.

### What You Write: Workers

The pipeline splits image generation into five focused workers, from prompt refinement through content validation, each handling one phase of the creative process.

| Worker | Task | What It Does |
|---|---|---|
| **PromptWorker** | `aig_prompt` | Engineers the user prompt for optimal model output. processes prompt text and style parameters, returns a refined prompt with token count |
| **GenerateWorker** | `aig_generate` | Generates the image at 1024x1024 resolution using the processed prompt and style settings | **Real (DALL-E 3)** when `CONDUCTOR_OPENAI_API_KEY` is set, Demo otherwise |
| **EnhanceWorker** | `aig_enhance` | Upscales the generated image and applies color correction for final output quality |
| **ValidateWorker** | `aig_validate` | Validates content safety and quality (quality score: 0.94, safe: true) against specifications |
| **DeliverWorker** | `aig_deliver` | Delivers the final image as PNG format to the specified output destination |

**Live mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `GenerateWorker` calls the real OpenAI DALL-E 3 API to generate images. The response includes `imageUrl` (a temporary URL to the generated image) and `revisedPrompt` (DALL-E's refined version of your prompt). When the key is not set, the worker runs in demo mode with `[DEMO]` prefixed output.

The remaining demo workers produce realistic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call. the worker interface stays the same, and no workflow changes are needed.

### The Workflow

```
aig_prompt
 │
 ▼
aig_generate
 │
 ▼
aig_enhance
 │
 ▼
aig_validate
 │
 ▼
aig_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
