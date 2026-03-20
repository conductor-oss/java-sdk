# AI Image Generation in Java Using Conductor :  Prompt Engineering, Generation, Enhancement, Validation, Delivery

A Java Conductor workflow that generates images from text prompts through a five-stage pipeline .  engineering the prompt with style-specific keywords, generating the image with a diffusion model, enhancing resolution and color, validating content safety and quality, and delivering the final output. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate prompt engineering, generation, enhancement, validation, and delivery as independent workers ,  you write the image generation logic, Conductor handles sequencing, retries, durability, and observability for free.

## From Text Prompt to Production-Ready Image

A user requests "a photorealistic mountain landscape at sunset, 4K resolution." The raw prompt needs enrichment with model-specific keywords (lighting terms, style modifiers, negative prompts) before the diffusion model produces good results. The generated image may need upscaling to the requested resolution. Before delivery, the image must pass content safety checks (no unintended artifacts, NSFW detection) and specification validation (correct resolution, aspect ratio, style match).

Each step has different failure modes .  prompt engineering might produce conflicting style directives, the model might timeout on high-resolution generation, the enhancer might introduce artifacts, and the validator might reject the image. Without orchestration, a failed generation means re-engineering the prompt, and there's no record of which prompt version produced which image.

## The Solution

**You just write the prompt engineering, image generation, enhancement, content validation, and delivery logic. Conductor handles retries, step sequencing, and full audit trails for every generated image.**

`PromptWorker` transforms the user's description into an optimized prompt with model-specific keywords, style modifiers, negative prompts, and technical parameters. `GenerateWorker` sends the engineered prompt to the image generation model and returns the raw output. `EnhanceWorker` upscales the image to the target resolution and applies quality improvements. `ValidateWorker` checks content safety, resolution compliance, and style adherence. `DeliverWorker` packages the validated image with metadata (prompt used, model version, generation parameters) and delivers it. Conductor records the full prompt-to-image chain for reproducibility.

### What You Write: Workers

The pipeline splits image generation into five focused workers, from prompt refinement through content validation, each handling one phase of the creative process.

| Worker | Task | What It Does |
|---|---|---|
| **PromptWorker** | `aig_prompt` | Engineers the user prompt for optimal model output .  processes prompt text and style parameters, returns a refined prompt with token count |
| **GenerateWorker** | `aig_generate` | Generates the image at 1024x1024 resolution using the processed prompt and style settings | **Real (DALL-E 3)** when `CONDUCTOR_OPENAI_API_KEY` is set, Simulated otherwise |
| **EnhanceWorker** | `aig_enhance` | Upscales the generated image and applies color correction for final output quality |
| **ValidateWorker** | `aig_validate` | Validates content safety and quality (quality score: 0.94, safe: true) against specifications |
| **DeliverWorker** | `aig_deliver` | Delivers the final image as PNG format to the specified output destination |

**Live mode:** When `CONDUCTOR_OPENAI_API_KEY` is set, `GenerateWorker` calls the real OpenAI DALL-E 3 API to generate images. The response includes `imageUrl` (a temporary URL to the generated image) and `revisedPrompt` (DALL-E's refined version of your prompt). When the key is not set, the worker runs in simulated mode with `[SIMULATED]` prefixed output.

The remaining simulated workers produce realistic output shapes so the workflow runs end-to-end. To go to production, replace the simulation with the real API call .  the worker interface stays the same, and no workflow changes are needed.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Example 809: AI Image Generation. Prompt, Generate, Enhance, Validate, Deliver ===

Step 1: Registering task definitions...
  Registered: aig_prompt, aig_generate, aig_enhance, aig_validate, aig_deliver

Step 2: Registering workflow 'aig_image_generation'...
  Workflow registered.

Step 3: Starting workers...
  5 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [deliver] Processing
  [enhance] Processing
  [generate] [SIMULATED] Image generated at 1024x1024 resolution
  [prompt] Processing
  [validate] Processing

  Status: COMPLETED
  Output: {delivered=..., url=..., sizeKB=..., enhancedImageId=...}

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/ai-image-generation-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |
| `CONDUCTOR_OPENAI_API_KEY` | *(unset)* | OpenAI API key. When set, `GenerateWorker` calls the real DALL-E 3 API to generate images. When unset, it runs in simulated mode. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/ai-image-generation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow aig_image_generation \
  --version 1 \
  --input '{"prompt": "sample-prompt", "A futuristic city at sunset": "sample-A futuristic city at sunset", "style": "sample-style", "photorealistic": "sample-photorealistic", "resolution": "sample-resolution"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w aig_image_generation -s COMPLETED -c 5
```

## How to Extend

Swap each worker for your real image pipeline. Stable Diffusion for generation, Real-ESRGAN for upscaling, AWS Rekognition for content moderation, and the workflow runs identically in production.

- **GenerateWorker** (`aig_generate`): already integrated with DALL-E 3 (set `CONDUCTOR_OPENAI_API_KEY`). For alternatives, swap to Stable Diffusion via Replicate/RunPod, or Midjourney API with model-specific parameter tuning
- **EnhanceWorker** (`aig_enhance`): use Real-ESRGAN or Topaz Gigapixel APIs for super-resolution, or GFPGAN for face enhancement in portrait images
- **ValidateWorker** (`aig_validate`): integrate AWS Rekognition for content moderation, CLIP for style/prompt adherence scoring, or custom classifiers for brand-specific content policies

Each worker maintains its contract, so swapping a local model for a cloud API requires no pipeline changes.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>
```

## Project Structure

```
ai-image-generation-ai-image-generation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/aiimagegeneration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── AiImageGenerationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── DeliverWorker.java
│       ├── EnhanceWorker.java
│       ├── GenerateWorker.java
│       ├── PromptWorker.java
│       └── ValidateWorker.java
└── src/test/java/aiimagegeneration/workers/
    ├── PromptWorkerTest.java        # 1 tests
    └── ValidateWorkerTest.java        # 1 tests
```
