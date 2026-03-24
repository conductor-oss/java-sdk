# Sequential Tool Chain: Search, Read Page, Extract Data, Summarize

Four tools in strict sequence: search returns URLs with titles and snippets, read fetches the page content (with `sections` list), extract pulls facts (`List.of(...)`) and key features, and summarize produces the final output with a confidence score.

## Workflow

```
query, maxResults -> ts_search_web -> ts_read_page -> ts_extract_data -> ts_summarize
```

## Tests

35 tests cover web search, page reading, data extraction, and summarization.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
