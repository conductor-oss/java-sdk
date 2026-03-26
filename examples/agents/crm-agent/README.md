# CRM Agent: Lookup Customer, Check History, Update Record, Respond

A support inquiry arrives for a customer. The agent looks up the customer ("Acme Corporation", support@acme.com), checks recent issue history (e.g., technical issue on 2026-02-15), updates the CRM record with a ticket (TKT-FIXED-001), and generates a prioritized response.

## Workflow

```
customerId, inquiry, channel -> cm_lookup_customer -> cm_check_history -> cm_update_record -> cm_generate_response
```

## Workers

**LookupCustomerWorker** (`cm_lookup_customer`) -- Returns `name: "Acme Corporation"`, `email: "support@acme.com"`.

**CheckHistoryWorker** (`cm_check_history`) -- Returns recent issues: `{date: "2026-02-15", type: "technical", ...}`.

**UpdateRecordWorker** (`cm_update_record`) -- Creates `ticketId: "TKT-FIXED-001"`, `recordUpdated: true`.

**GenerateResponseWorker** (`cm_generate_response`) -- Builds response with priority level.

## Tests

34 tests cover customer lookup, history checking, record updates, and response generation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
