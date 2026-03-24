# Detect, Diagnose, Fix, and Verify Production Incidents

An incident is detected (affected endpoints: `["/api/checkout", "/api/payment"]`), diagnosed (analyzing endpoint count and patterns), a fix is suggested and executed, then the fix is verified.

## Workflow

```
alertId, serviceName, severity
  -> iai_detect -> iai_diagnose -> iai_suggest_fix -> iai_execute_fix -> iai_verify
```

## Tests

10 tests cover detection, diagnosis, fix suggestion, execution, and verification.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
