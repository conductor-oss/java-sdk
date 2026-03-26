# API-Calling Agent: Plan, Authenticate, Call, Parse, Format

A user asks a question that requires calling an external API. The agent plans which API to call (e.g., GitHub), authenticates to get a token, makes the HTTP call to `https://api.github.com/repos/conductor-oss/conductor`, parses the response (extracting `full_name` into `name`), and formats the output with `dataSource: apiName + "_api"`. Body previews are truncated to 2000 chars.

## Workflow

```
userRequest, apiCatalog -> ap_plan_api_call -> ap_authenticate -> ap_call_api -> ap_parse_response -> ap_format_output
```

## Workers

**PlanApiCallWorker** (`ap_plan_api_call`) -- Selects the API and builds params + response schema.

**AuthenticateWorker** (`ap_authenticate`) -- Returns `token` and `expiresIn`.

**CallApiWorker** (`ap_call_api`) -- Calls `https://api.github.com/repos/conductor-oss/conductor`. Truncates body at 2000 chars.

**ParseResponseWorker** (`ap_parse_response`) -- Extracts `full_name` -> `name` from GitHub responses.

**FormatOutputWorker** (`ap_format_output`) -- Formats as `answer` with `dataSource`.

## Tests

40 tests cover API planning, authentication, live HTTP calls, response parsing, and formatting.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
