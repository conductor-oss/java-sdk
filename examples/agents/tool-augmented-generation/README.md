# Tool-Augmented Generation: Fill Knowledge Gaps During Text Generation

A language model starts generating text about Node.js but hits a factual boundary -- it needs the current LTS version number, which changes over time and may not be in its training data. Generating a hallucinated version number is worse than admitting the gap. The model needs to pause mid-generation, call an external tool, incorporate the verified fact, and resume.

This workflow demonstrates the tool-augmented generation pattern: start generating, detect a knowledge gap, invoke an external tool to fill it, merge the result back into the text, and complete the generation.

## Pipeline Architecture

```
prompt
  |
  v
tg_start_generation      (partialText, gapType="factual_lookup", needsTool=true)
  |
  v
tg_detect_gap            (toolName="version_lookup", toolQuery, gapLocation="end")
  |
  v
tg_call_tool             (toolResult, source="nodejs.org", cached=false)
  |
  v
tg_incorporate_result    (enrichedText with verified fact inserted)
  |
  v
tg_complete_generation   (finalText, totalTokens=87)
```

## Worker: StartGeneration (`tg_start_generation`)

Produces partial text: `"Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is"` -- deliberately stopping at the knowledge boundary. Flags the gap as `gapType: "factual_lookup"` and `needsTool: true`.

## Worker: DetectGap (`tg_detect_gap`)

Analyzes the partial text and gap type. Determines that a `"version_lookup"` tool should be called with query `"Node.js current LTS version"`. Reports `gapLocation: "end"` to indicate where the fact should be spliced in.

## Worker: CallTool (`tg_call_tool`)

Invokes the external tool identified by name and query. Returns `toolResult: "Node.js v22.x (Jod) is the current LTS version as of 2025"` with `source: "nodejs.org"` and `cached: false`.

## Worker: IncorporateResult (`tg_incorporate_result`)

Merges the tool result into the partial text, producing enriched text: `"Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is v22.x (Jod), as confirmed by official sources."` The merge replaces the gap with the verified factual statement.

## Worker: CompleteGeneration (`tg_complete_generation`)

Appends the remaining generated content to the enriched text, adding context about Node.js's event-driven, non-blocking I/O model. Reports `totalTokens: 87` for the complete output.

## Tests

5 tests cover generation start, gap detection, tool invocation, incorporation, and completion.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
