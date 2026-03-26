# AI Image Generation: Prompt Engineering Through Delivery

A designer needs to generate an image from a text description, but raw prompts rarely produce high-quality results on the first try. The prompt needs refinement for the target style, the generated image needs upscaling and color correction, and the final output must be validated for content safety and artifact detection before delivery to the user.

This workflow chains five stages: prompt processing, image generation (with optional DALL-E 3 integration), enhancement, quality validation, and delivery.

## Pipeline Architecture

```
prompt, style, resolution
       |
       v
aig_prompt               (processedPrompt with style suffix, tokens=42)
       |
       v
aig_generate             (imageId, imageUrl, revisedPrompt)
       |
       v
aig_enhance              (enhancedImageId, upscaleFactor=2)
       |
       v
aig_validate             (qualityScore=0.94, safeContent, artifacts=0)
       |
       v
aig_deliver              (delivered=true, url as PNG path, sizeKB=2048)
```

## Worker: Prompt (`aig_prompt`)

Appends style and quality modifiers to the raw prompt: `"{prompt}, {style} style, highly detailed, 8k resolution"`. Returns the `processedPrompt` and a token estimate of `42`.

## Worker: Generate (`aig_generate`)

Attempts to call the OpenAI DALL-E 3 API at `https://api.openai.com/v1/images/generations` if an API key is available. Sends the processed prompt with the specified resolution as a JSON POST request. Parses the response to extract `imageUrl` and `revisedPrompt` from `data[0]`. On success, returns `imageId: "IMG-dalle3-{timestamp}"`. If no API key is configured or the API returns an error, falls back to a deterministic placeholder URL at `"https://example.com/deterministic.image.png"`. Error details (HTTP status, response body) are captured in the output.

## Worker: Enhance (`aig_enhance`)

Upscales and color-corrects the generated image. Appends `-E` to the image ID to produce `enhancedImageId: "IMG-ai-image-generation-001-E"`. Returns `upscaleFactor: 2`.

## Worker: Validate (`aig_validate`)

Checks the enhanced image for quality and safety. Returns `qualityScore: 0.94`, `safeContent: true`, and `artifacts: 0` (no visual artifacts detected).

## Worker: Deliver (`aig_deliver`)

Delivers the final image as a PNG file. Returns `delivered: true`, `url: "/images/IMG-ai-image-generation-001-E.png"`, and `sizeKB: 2048`.

## Tests

2 tests cover the image generation pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
