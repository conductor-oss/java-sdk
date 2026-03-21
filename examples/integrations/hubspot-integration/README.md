# Hubspot Integration in Java Using Conductor

## Onboarding Contacts with Enrichment and Assignment

When a new lead arrives, they need to be created as a contact in HubSpot, their profile needs to be enriched with company intelligence (industry, company size, revenue segment), a sales rep needs to be assigned based on the enriched profile, and the contact should be enrolled in an appropriate nurture sequence. Each step depends on the previous one. you cannot enrich without a contact ID, and you cannot assign an owner without knowing the industry and company size.

Without orchestration, you would chain HubSpot API calls manually, pass contact IDs between steps, and manage the enrichment-to-assignment logic yourself. Conductor sequences the pipeline and routes contact IDs, enrichment data, and owner assignments between workers automatically.

## The Solution

**You just write the HubSpot workers. Contact creation, profile enrichment, owner assignment, and nurture sequence enrollment. Conductor handles contact-to-nurture sequencing, CRM API retries, and contact ID routing across enrichment and assignment stages.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers onboard CRM contacts: CreateContactWorker adds the lead, EnrichDataWorker augments with company intelligence, AssignOwnerWorker routes to the right sales rep, and NurtureSequenceWorker enrolls in drip campaigns.

| Worker | Task | What It Does |
|---|---|---|
| **CreateContactWorker** | `hs_create_contact` | Creates a contact in HubSpot. takes the email, name, and company and returns the contact ID |
| **EnrichDataWorker** | `hs_enrich_data` | Enriches the contact profile. looks up company intelligence (industry, company size, revenue segment) using the contact ID and company name |
| **AssignOwnerWorker** | `hs_assign_owner` | Assigns a sales rep owner. routes the contact to the appropriate rep based on the enriched profile (industry, company size, territory) |
| **NurtureSequenceWorker** | `hs_nurture_sequence` | Enrolls the contact in a nurture sequence. selects the appropriate email drip campaign based on the contact's profile and stage |

the workflow orchestration and error handling stay the same.

### The Workflow

```
hs_create_contact
 │
 ▼
hs_enrich_data
 │
 ▼
hs_assign_owner
 │
 ▼
hs_nurture_sequence

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
