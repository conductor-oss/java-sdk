# AI Video Generation in Java Using Conductor : Script, Storyboard, Generate, Edit, Render

## Automated Video Production Is a Multi-Stage Pipeline

Creating a 60-second video about "sustainable energy" requires five distinct production stages: writing a script with narration and scene descriptions, translating the script into a visual storyboard (camera angles, compositions, visual elements per scene), generating video clips for each storyboard frame, editing the clips into a coherent sequence with transitions and timing, and rendering the final output at the target resolution and format.

Each stage is a different skillset and toolset. NLP for scriptwriting, visual planning for storyboarding, diffusion models for video generation, editing algorithms for sequencing, and encoding for rendering. If video generation fails on scene 3 of 5, you need to retry that scene without re-running the scriptwriting and storyboarding that already succeeded.

## The Solution

**You just write the scriptwriting, storyboarding, clip generation, editing, and rendering logic. Conductor handles render retries, stage dependencies, and execution tracking across all video pipeline steps.**

`ScriptWorker` writes the video script from the topic. narration text, scene descriptions, timing cues, and visual directions for the specified duration and style. `StoryboardWorker` translates each script scene into detailed visual specifications, composition, camera movement, color palette, and transition type. `GenerateWorker` creates video clips for each storyboard frame using AI video generation models. `EditWorker` sequences the clips with transitions, timing adjustments, and audio sync. `RenderWorker` encodes the final video at the target resolution, format, and bitrate. Conductor tracks each stage's execution time and output for production analytics.

### What You Write: Workers

Five workers divide the video creation process: scripting, storyboarding, clip generation, editing, and rendering, so each stage can scale and retry independently.

| Worker | Task | What It Does |
|---|---|---|
| **EditWorker** | `avg_edit` | Edits the generated video clips. applies transitions, audio sync, and timing adjustments to produce a coherent sequence |
| **GenerateWorker** | `avg_generate` | Generates video clips for each storyboard scene using AI video generation in the specified style |
| **RenderWorker** | `avg_render` | Renders the edited video at 1080p resolution, producing the final MP4 output with encoding metadata |
| **ScriptWorker** | `avg_script` | Writes a video script from the topic. generates scene descriptions, narration, and timing cues for the specified duration |
| **StoryboardWorker** | `avg_storyboard` | Translates the script into a visual storyboard with 5 scenes, shot descriptions, and 15 keyframes |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
avg_script
 │
 ▼
avg_storyboard
 │
 ▼
avg_generate
 │
 ▼
avg_edit
 │
 ▼
avg_render

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
