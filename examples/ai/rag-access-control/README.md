# RAG with Authentication, Permission Filtering, and PII Redaction

An engineer asks a question that spans public docs and confidential HR data. Without access control, the RAG system leaks salary information to anyone who asks. This pipeline authenticates the user, checks role-based permissions, retrieves only permitted documents, redacts PII (SSNs and salary figures), then generates the answer.

## Workflow

```
question, userId
       │
       ▼
┌──────────────────────────┐
│ ac_authenticate_user     │  Verify user, return roles + clearance
└───────────┬──────────────┘
            ▼
┌──────────────────────────┐
│ ac_check_permissions     │  Evaluate role-based access
└───────────┬──────────────┘
            ▼
┌──────────────────────────┐
│ ac_filtered_retrieve     │  Retrieve only accessible documents
└───────────┬──────────────┘
            ▼
┌──────────────────────────┐
│ ac_redact_sensitive      │  Strip SSNs and salary figures
└───────────┬──────────────┘
            ▼
┌──────────────────────────┐
│ ac_generate              │  Generate answer from redacted context
└──────────────────────────┘
```

## Workers

**AuthenticateUserWorker** (`ac_authenticate_user`) -- Returns `roles: ["engineer", "team-lead"]` and clearance level for the given `userId`.

**CheckPermissionsWorker** (`ac_check_permissions`) -- Evaluates the user's roles and clearance against required permissions for the requested resource.

**FilteredRetrieveWorker** (`ac_filtered_retrieve`) -- Maintains a document list spanning collections: `"public-docs"`, `"engineering-wiki"`, and restricted HR documents. Filters based on the user's permission level, returning only documents the user is authorized to access.

**RedactSensitiveWorker** (`ac_redact_sensitive`) -- Uses two regex patterns: `SSN_PATTERN` matching `\d{3}-\d{2}-\d{4}` (replaced with `[SSN REDACTED]`) and `SALARY_PATTERN` matching `\$[\d,]+(?:\.\d{2})?` (replaced with `[SALARY REDACTED]`).

**GenerateWorker** (`ac_generate`) -- Detects whether the context contains redaction markers (`[SSN REDACTED]` or `[SALARY REDACTED]`) and adjusts the generation prompt accordingly. When `CONDUCTOR_OPENAI_API_KEY` is set, calls the LLM with the sanitized context.

## Tests

15 tests cover authentication, permission checking, document filtering, PII redaction patterns, and redaction-aware generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
