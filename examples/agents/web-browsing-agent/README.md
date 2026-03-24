# Web Browsing Agent: Plan Search, Execute, Select Pages, Read, Extract

The agent plans search queries from the question, executes the search (returning results with URL, title, snippet, and relevance score), selects the most relevant pages by score, reads their content, and extracts an answer with confidence score and word count.

## Workflow

```
question, maxPages
  -> wb_plan_search -> wb_execute_search -> wb_select_pages -> wb_read_page -> wb_extract_answer
```

## Workers

**PlanSearchWorker** (`wb_plan_search`) -- Generates `searchQueries` from the question.

**ExecuteSearchWorker** (`wb_execute_search`) -- Returns results with relevance scores.

**SelectPagesWorker** (`wb_select_pages`) -- Filters by relevance score.

**ReadPageWorker** (`wb_read_page`) -- Reads selected page content from URLs.

**ExtractAnswerWorker** (`wb_extract_answer`) -- Produces `answer`, `sources`, `confidence`, and `wordCount`.

## Tests

44 tests cover search planning, execution, page selection, reading, and answer extraction.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
