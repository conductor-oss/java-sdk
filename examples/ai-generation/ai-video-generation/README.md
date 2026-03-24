# Video Generation: Script, Storyboard, Generate, Edit, Render

Produce a video from a topic: write the script (via LLM), create a storyboard (via LLM), generate video segments, edit them together, and render the final output.

## Workflow

```
topic, duration, style
  -> avg_script -> avg_storyboard -> avg_generate -> avg_edit -> avg_render
```

## Tests

2 tests cover the video generation pipeline.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
