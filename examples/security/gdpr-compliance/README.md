# GDPR Compliance: Regex-Based PII Detection, Data Source Scanning, and Auditable Erasure

Under GDPR Article 17, when a user requests data erasure, you have 30 days to find and remove their personal data from every system. Not just your primary database -- also billing, analytics, logs, and third-party vendors. Miss one, and you face regulatory fines. This example implements a GDPR data subject request pipeline using [Conductor](https://github.com/conductor-oss/conductor) that verifies identity, scans real data structures for subject matches, detects and masks PII using four regex patterns (email, SSN, phone, credit card), and produces an auditable completion report with a unique `GDPR-` report ID.

## The Pipeline

```
gdpr_verify_identity  -->  gdpr_locate_data  -->  gdpr_process_request  -->  gdpr_confirm_completion
```

The workflow accepts `subjectId` and `requestType`. Each worker produces an `auditLog` map with `timestamp`, `action`, `actor`, `result`, and `detail` -- creating a regulator-ready chain of evidence.

## Identity Verification

`VerifyIdentityWorker` requires `requestorId` and `email`. Email validation checks for both `@` and `.` characters. On failure, the audit log captures the specific failure reason (e.g., `"Invalid email format"`) with the requestor ID as the actor. This prevents unauthorized data access requests -- a critical GDPR requirement.

## Data Location: Real Record Scanning

`LocateDataWorker` is the most complex worker. It accepts a `subjectId` and a `dataSources` list -- each entry is a map with `system`, `table`, `fields`, and `records`. The worker scans every field of every record in every data source, checking if the value contains the subject ID:

```java
for (Map.Entry<String, Object> entry : record.entrySet()) {
    if (entry.getValue() != null && entry.getValue().toString().contains(subjectId)) {
        if (!matchedFields.contains(entry.getKey())) {
            matchedFields.add(entry.getKey());
        }
    }
}
```

The output includes `dataLocations` (which systems contained the subject's data), `systemsScanned` (total systems checked), and `fieldsMatched` (total fields containing the subject's data). In the integration test, scanning `USER-42` across a `user_db.profiles` table and a `billing_db.payments` table correctly identifies matches in both systems.

## PII Detection and Masking

`ProcessRequestWorker` supports four request types: `access`, `erasure`, `anonymize`, `portability`. It scans raw text data against four compiled regex patterns:

```java
static final Pattern EMAIL_PATTERN       = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
static final Pattern SSN_PATTERN         = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
static final Pattern PHONE_PATTERN       = Pattern.compile("\\b(\\+?\\d{1,3}[-.]?)?\\(?\\d{3}\\)?[-.]?\\d{3}[-.]?\\d{4}\\b");
static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");
```

The phone pattern handles US formats (`555-123-4567`), parenthesized area codes (`(555)123-4567`), and international prefixes (`+44.207.123.4567`).

For `access` and `portability` requests, the original data is returned unchanged -- but all PII is still detected and reported. For `erasure` and `anonymize` requests, PII is replaced with tokens: `[EMAIL_REDACTED]`, `[SSN_REDACTED]`, `[PHONE_REDACTED]`, `[CC_REDACTED]`.

Individual PII items are reported with their type, a masked value (first 2 and last 2 characters visible, middle replaced with `*`), and the character position where they were found.

## Completion Confirmation

`ConfirmCompletionWorker` generates a unique report ID: `"GDPR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`. It records the request type, PII items processed count, and completion timestamp. The `piiCount` must be a non-negative integer -- negative values are rejected with a terminal error. The `piiCount` input supports both `Number` and `String` types, parsing strings with `Integer.parseInt()` and rejecting non-numeric strings with a specific error message.

## Audit Log Structure

Every worker outputs an `auditLog` map using the shared `VerifyIdentityWorker.auditLog()` static method. This ensures consistent structure across the pipeline:

```java
static Map<String, String> auditLog(String action, String actor, String resultStatus, String detail) {
    Map<String, String> log = new LinkedHashMap<>();
    log.put("timestamp", Instant.now().toString());
    log.put("action", action);
    log.put("actor", actor);
    log.put("result", resultStatus);
    log.put("detail", detail);
    return log;
}
```

The `action` field identifies the step (`gdpr_verify_identity`, `gdpr_locate_data`, etc.). The `actor` is the requestor ID on success or `"SYSTEM"` on validation failures. The `result` is `"SUCCESS"` or `"FAILED"`. The `detail` provides human-readable context like `"erasure completed, 3 PII items processed, report: GDPR-A1B2C3D4"`. Even failed steps produce audit logs, so you can prove to regulators that a request was received and rejected for a valid reason (e.g., invalid email format).

## Test Coverage

**MainExampleTest** (30 tests):
- PII detection: email, SSN, US phone, international phone (`+44.207.123.4567`), credit card, multiple PII types in one string, clean data returns zero, partial SSN not detected
- Masking: erasure replaces PII with redaction tokens, anonymize masks data, access preserves original
- Input validation: missing/invalid request type, missing data, audit log structure
- Identity: successful verification, missing requestor ID, missing email, invalid email format
- Data location: missing subject ID, missing data sources, real record scanning, no-match returns empty
- Completion: success with report ID prefix check, missing request type/subject ID/PII count, negative PII count

**GdprIntegrationTest** (3 tests):
- Full erasure pipeline: verify -> locate across 2 databases -> process with 3+ PII elements detected -> confirm with report ID
- Pipeline stops on verification failure
- Access request preserves original data while still detecting PII

**33 tests total** with the workflow timeout set to 86,400 seconds (24 hours) to accommodate the 30-day GDPR compliance window.

---

> **How to run:** See [RUNNING.md](../../RUNNING.md) | **Production guidance:** See [PRODUCTION.md](PRODUCTION.md)
