# RAG Access Control in Java Using Conductor : Authentication, Permission Filtering, and Data Redaction

## RAG Without Access Control Is a Data Leak

A standard RAG pipeline retrieves documents and generates answers without checking who's asking. In an enterprise, this means an intern can ask questions about board-level financial documents, a contractor can access employee HR records, and sensitive customer PII shows up in generated responses. The vector store doesn't know about your org chart or data classification policies.

Access-controlled RAG adds four gates before generation: authenticate the user (validate their token/session), check their permissions (which document categories can they access?), filter retrieval results (remove any documents they shouldn't see), and redact sensitive fields from the remaining documents. Only then does the sanitized context reach the LLM.

Each gate must execute in order. you can't filter by permissions before you know who the user is, and you can't redact before filtering. If any gate fails (invalid token, permission service unavailable), the pipeline must stop cleanly without leaking data.

## The Solution

**You write the authentication, permission filtering, and data redaction logic. Conductor handles the access-controlled pipeline, retries, and observability.**

Each access control gate is an independent worker. authentication, permission checking, filtered retrieval, sensitive data redaction, and generation. Conductor sequences them so each gate's output feeds into the next. If the permission service times out, Conductor retries it without re-authenticating. Every query is tracked with the user identity, permissions applied, documents filtered, fields redacted, and final answer, creating a complete audit trail for compliance.

### What You Write: Workers

Five workers enforce access control throughout the RAG pipeline. authenticating the user, checking document-level permissions, retrieving only authorized content, redacting sensitive fields, and generating an answer from the sanitized context.

| Worker | Task | What It Does |
|---|---|---|
| **AuthenticateUserWorker** | `ac_authenticate_user` | Worker that authenticates a user by validating their auth token. Returns authenticated status, roles, and clearance l... |
| **CheckPermissionsWorker** | `ac_check_permissions` | Worker that checks user permissions based on roles and clearance level. Determines which document collections the use... |
| **FilteredRetrieveWorker** | `ac_filtered_retrieve` | Worker that retrieves documents filtered by the user's allowed collections. Contains a corpus of 5 documents across d... |
| **GenerateWorker** | `ac_generate` | Worker that generates an answer from the access-controlled and redacted context. Notes when data has been redacted du... |
| **RedactSensitiveWorker** | `ac_redact_sensitive` | Worker that redacts sensitive information from documents based on clearance level. Redacts SSN patterns and salary fi... |

Workers implement LLM API responses with realistic outputs so you can run the full pipeline without API keys. Set the provider API key environment variable to switch to live mode. the workflow and worker interfaces stay the same.

### The Workflow

```
ac_authenticate_user
 │
 ▼
ac_check_permissions
 │
 ▼
ac_filtered_retrieve
 │
 ▼
ac_redact_sensitive
 │
 ▼
ac_generate

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
