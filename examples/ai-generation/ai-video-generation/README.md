# AI Video Generation: Script to Final Render

A marketing team needs short-form video content for a product launch but lacks an in-house video production crew. Creating a video from scratch requires scriptwriting, storyboarding, footage generation, editing, and rendering -- each demanding specialized skills. Automating this pipeline lets the team produce videos by providing just a topic, target duration, and visual style.

This workflow models the five stages of video production from script to final rendered output.

## Pipeline Architecture

```
topic, duration, style
       |
       v
avg_script               (script text, scenes=5, wordCount=320)
       |
       v
avg_storyboard           (storyboard, scenes=5, keyframes=15)
       |
       v
avg_generate             (videoId, clips=5, rawDuration=35 sec)
       |
       v
avg_edit                 (editedVideoId, edits=12)
       |
       v
avg_render               (finalVideoId, duration=30, format="mp4", complete=true)
```

## Worker: Script (`avg_script`)

Writes a video script based on the `topic` and target `duration`. When an LLM is available, generates a full narrative script via the model and counts words with `split("\\s+").length`. Falls back to `script: "scene-script-5-scenes"`, `scenes: 5`, and `wordCount: 320`. The scene count determines how many segments the generator will produce.

## Worker: Storyboard (`avg_storyboard`)

Converts the script into visual planning. When an LLM is available, generates detailed shot descriptions per scene. Falls back to `storyboard: "storyboard-5-scenes"`, `scenes: 5`, and `keyframes: 15` (averaging 3 keyframes per scene). The keyframe count guides the video generator.

## Worker: Generate (`avg_generate`)

Produces raw video clips from the storyboard. Returns `videoId: "VID-ai-video-generation-001"`, `clips: 5` (one per scene), and `rawDuration: 35` seconds. The raw duration exceeds the target to allow trimming during editing.

## Worker: Edit (`avg_edit`)

Edits the generated clips: trimming, transitions, color grading, and audio syncing. Returns `editedVideoId: "VID-ai-video-generation-001-E"` (appending `-E`) and `edits: 12` individual edit operations applied.

## Worker: Render (`avg_render`)

Renders the edited video into the final format. Returns `finalVideoId: "VID-ai-video-generation-001-F"` (appending `-F`), `duration: 30` seconds (trimmed from the raw 35), `format: "mp4"`, and `complete: true`.

## Tests

2 tests cover the video generation pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
