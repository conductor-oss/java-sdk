# Compliance Vendor

Vendor compliance: assess, audit, certify, and monitor.

**Input:** `vendorId`, `vendorName`, `complianceStandard` | **Timeout:** 60s

## Pipeline

```
vcm_assess
    │
vcm_audit
    │
vcm_certify
    │
vcm_monitor
```

## Workers

**AssessWorker** (`vcm_assess`)

Reads `complianceStandard`, `vendorId`. Outputs `assessmentResult`, `score`.

**AuditWorker** (`vcm_audit`)

Reads `assessmentResult`. Outputs `passed`, `findings`, `critical`.

**CertifyWorker** (`vcm_certify`)

```java
String certId = auditPassed ? "CERT-665-001" : null;
```

Reads `auditPassed`. Outputs `certificationId`, `validUntil`.

**MonitorWorker** (`vcm_monitor`)

Reads `vendorId`. Outputs `active`, `checkInterval`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
