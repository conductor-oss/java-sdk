# Two-Agent Pipeline: Writer and Editor in Sequence

A content team needs a quick draft polished into publication-ready prose. The writer focuses on getting ideas down fast without worrying about grammar or tone; the editor focuses exclusively on refinement without the cognitive burden of generating ideas from scratch. Splitting these responsibilities produces better output than asking one person to do both simultaneously.

This is the simplest multi-agent pattern: two agents in sequence with a final assembly step that packages the complete pipeline result.

## Pipeline Architecture

```
topic, tone
    |
    v
tap_writer_agent        (draft string, wordCount=35, model)
    |
    v
tap_editor_agent        (editedContent, notes, changesCount=4, model)
    |
    v
tap_final_output        (finalContent, summary with metadata)
```

## Worker: WriterAgent (`tap_writer_agent`)

Takes `topic`, `tone`, and `systemPrompt` via `getInputData().getOrDefault()` with defaults of `"technology"`, `"professional"`, and `"You are a skilled writer."`. Generates a four-sentence draft that interpolates both the topic and tone. Returns `draft` (the full text), `wordCount: 35`, and `model: "writer-agent-v1"`.

## Worker: EditorAgent (`tap_editor_agent`)

Receives the writer's `draft` and the target `tone`. Prepends `"EDITED: "` to the draft and appends a bracketed refinement note: `"[Refined for clarity, coherence, grammar, and {tone} tone]"`. Produces four specific `notes`: improved sentence structure, corrected grammar, enhanced vocabulary, and added transitional phrases. Returns `editedContent`, `notes`, `changesCount: 4`, and `model: "editor-agent-v1"`.

## Worker: FinalOutput (`tap_final_output`)

Assembles the pipeline result from `originalDraft`, `editedContent`, and `editorNotes`. Computes `originalLength` and `editedLength` as character counts via `.length()`. Returns `finalContent` (the edited version) and a `summary` map containing both lengths, the editor notes, and `agentsPipeline: ["writer-agent-v1", "editor-agent-v1"]` for traceability.

## Tests

3 tests cover writing, editing, and final output assembly.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
