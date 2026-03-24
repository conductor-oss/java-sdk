# Double-Validated Tool Calls: Input and Output Validation

Tool calls are validated at both ends. The generator builds `toolArgs` (including `"include": ["temperature", "humidity", "wind", "forecast"]`). Input validation runs checks (`List.of(...)` validation entries). Execution returns `rawOutput` with `"forecast": List.of(...)`. Output validation runs its own checks. Finally, the result is formatted and delivered.

## Workflow

```
userRequest, toolName
  -> tv_generate_tool_call -> tv_validate_input -> tv_execute_tool -> tv_validate_output -> tv_deliver
```

## Tests

43 tests cover call generation, input validation, execution, output validation, and delivery.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
