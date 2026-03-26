# Hubspot Integration

Orchestrates hubspot integration through a multi-stage Conductor workflow.

**Input:** `email`, `firstName`, `lastName`, `company` | **Timeout:** 60s

## Pipeline

```
hs_create_contact
    │
hs_enrich_data
    │
hs_assign_owner
    │
hs_nurture_sequence
```

## Workers

**AssignOwnerWorker** (`hs_assign_owner`): Assigns a sales rep owner.

Reads `companySize`, `contactId`, `industry`. Outputs `ownerId`, `ownerName`.

**CreateContactWorker** (`hs_create_contact`): Creates a contact in HubSpot.

```java
.header("Authorization", "Bearer " + apiKey)
```

Reads `company`, `email`, `firstName`, `lastName`. Outputs `contactId`, `createdAt`.

**EnrichDataWorker** (`hs_enrich_data`): Enriches contact data.

Reads `contactId`. Outputs `industry`, `companySize`, `revenue`, `segment`.

**NurtureSequenceWorker** (`hs_nurture_sequence`): Enrolls a contact in a nurture sequence.

Reads `contactId`, `segment`. Outputs `sequenceName`, `enrolledAt`, `stepsCount`.

## Tests

**8 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
