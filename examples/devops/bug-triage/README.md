# Severity-Based Bug Triage with Automatic Routing

A bug report comes in with a title, description, and component. Someone has to read it,
classify its severity, route it to the right handler, and assign an engineer -- all before
the reporter loses faith. This workflow parses the report, classifies severity by keyword
matching, routes through a SWITCH to severity-specific handlers, and assigns the bug to the
right person.

## Workflow

```
bugReport {title, description, component}
       |
       v
+------------------+     +------------------------+
| btg_parse_report | --> | btg_classify_severity  |
+------------------+     +------------------------+
  bugId: BUG-2024-642      keyword scan on description
       |
       v
  SWITCH on severity
  +------------------+------------------+------------------+
  | critical         | high             | default (low)    |
  | btg_handle_      | btg_handle_      | btg_handle_      |
  | critical         | high             | low              |
  | -> paged_oncall  | -> next_sprint   | -> backlog       |
  +------------------+------------------+------------------+
       |
       v
  +-------------+
  | btg_assign  |   assignee by severity map
  +-------------+
```

## Workers

**ParseReportWorker** -- Extracts `title`, `description`, and `component` from the incoming
`bugReport` map. Assigns a deterministic `bugId` of `"BUG-2024-642"`.

**ClassifySeverityWorker** -- Lowercases the `description` and checks for keywords: `"crash"`
or `"data loss"` maps to `"critical"`, `"broken"` or `"error"` maps to `"high"`, everything
else is `"low"`.

**HandleCriticalWorker** -- Escalates the bug by paging on-call. Returns `action: "paged_oncall"`.

**HandleHighWorker** -- Flags the bug for the next sprint. Returns `action: "next_sprint"`.

**HandleLowWorker** -- Adds the bug to the backlog. Returns `action: "backlog"`.

**AssignWorker** -- Maps severity to an assignee using a fixed map: `"critical"` ->
`"senior-eng-1"`, `"high"` -> `"eng-2"`, `"low"` -> `"eng-3"`. Defaults to `"eng-3"`.

## Tests

12 unit tests cover parsing, classification, all three severity handlers, and assignment.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
