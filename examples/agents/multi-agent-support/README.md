# Customer Support: Classify Ticket, Route by Category, QA Validate

The classifier scans for keywords in the ticket: `error/crash/bug/broken/fail` routes to `"bug"`, and so on. A SWITCH routes by category: bugs go to knowledge search then solution proposal (referencing `["KB-1001", "KB-1002", "KB-1003"]`), features go to evaluation (priority: "high"), and general queries get suggested docs. Every response passes through QA validation checking `toneAppropriate` and `includesGreeting`.

## Workflow

```
ticketId, subject, description, customerTier
  -> cs_classify_ticket -> SWITCH(bug: cs_knowledge_search + cs_solution_propose, feature: cs_feature_evaluate, default: cs_general_respond) -> cs_qa_validate
```

## Tests

47 tests cover classification, all routing paths, and QA validation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
