# Writer-Editor Pipeline: Two Agents in Sequence

The simplest multi-agent pattern. The writer produces a `draft` (`wordCount: 35`). The editor refines it into `editedContent` with `notes`. A final output compiles both with `agentsPipeline: ["writer-agent-v1", "editor-agent-v1"]`.

## Workflow

```
topic -> tap_writer_agent -> tap_editor_agent -> tap_final_output
```

## Tests

18 tests cover writing, editing, and final output compilation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
