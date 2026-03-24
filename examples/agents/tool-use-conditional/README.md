# Tool Use Conditional: Regex Classification with Routed Execution

A user submits a query that could be a math problem, a coding request, or an information lookup. Sending every query to the same tool wastes resources and produces poor results -- a calculator cannot write code, and a code interpreter is overkill for simple arithmetic. The system needs to classify the query first and route it to the right specialist.

This workflow uses regex-based classification to categorize queries, then a SWITCH task routes to the matching tool: calculator, code interpreter, or web search.

## Pipeline Architecture

```
userQuery
    |
    v
tc_classify_query        (category, toolName, parsed fields)
    |
    v
SWITCH on category
    |          |           |
  math       code        search
    |          |           |
    v          v           v
tc_calculator  tc_interpreter  tc_web_search
```

## Worker: ClassifyQuery (`tc_classify_query`)

Uses two compiled `Pattern` constants. `MATH_PATTERN` matches digits with operators, `sqrt`, `square root`, `factorial`, `calculate`, or `compute` (case-insensitive). `CODE_PATTERN` matches `write`, `code`, `function`, `program`, `script`, `implement`, `debug`, or `algorithm`. If `MATH_PATTERN` matches, the category is `"math"` and `parsedExpression` is built by stripping the `calculate`/`compute` prefix. If `CODE_PATTERN` matches, the category is `"code"` and `detectLanguage()` checks for `"javascript"`, `"js "`, or `"node"` keywords, defaulting to `"python"`. Anything else falls through to `"search"`.

## Worker: Calculator (`tc_calculator`)

Handles math queries with keyword-driven branching. If the expression contains `"square root"` or `"sqrt"`, returns `12` (from `sqrt(144)`) with method `"square_root"` and three computational steps. If it contains `"factorial"`, returns `120` (from `5!`) with method `"factorial"`. Otherwise defaults to `42` with method `"arithmetic"`. Each result includes a `calculation` map with `expression`, `result`, `steps` (as `List<String>`), and `method`.

## Worker: Interpreter (`tc_interpreter`)

Generates code in the detected language. For Python, produces a Fibonacci function using the iterative `a, b = 0, 1` pattern. For JavaScript, produces the same algorithm with ES6 destructuring `[a, b] = [b, a + b]`. Both return `executionOutput: "fibonacci(10) = 55"`. The answer string combines the language label, code block, and output.

## Worker: WebSearch (`tc_web_search`)

Returns two search results as `List<Map<String, Object>>` with `title`, `url` (slug-ified from the query), and `snippet`. Reports `totalFound: 1250`. The answer string includes the top result title.

## Tests

4 tests cover regex classification and all three tool execution paths.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
