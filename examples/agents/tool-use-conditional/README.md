# Conditional Tool Routing: Math Pattern Detection via Regex

The classifier uses `Pattern.compile(MATH_PATTERN)` to detect math queries, separate patterns for code, and defaults to search. A SWITCH routes to the appropriate tool: the calculator (detecting `"square root"` / `"sqrt"` keywords, producing `steps` as `List.of(...)`), the interpreter, or web search (returning ranked results).

## Workflow

```
userQuery -> tc_classify_query -> SWITCH(math: tc_calculator, code: tc_interpreter, search: tc_web_search)
```

## Tests

36 tests cover regex-based classification and all three tool routes.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
