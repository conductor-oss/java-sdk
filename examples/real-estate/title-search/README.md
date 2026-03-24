# Title Search in Java with Conductor : Record Search, Ownership Verification, Lien Check, and Certification

## The Problem

You need to confirm that a property's title is clear before a sale can close. County records must be searched for the complete chain of title, current ownership must be verified against the recorded deeds, any outstanding liens (tax liens, mechanic's liens, HOA liens, judgments) must be identified, and only if ownership is verified and liens are clear can a title certificate be issued. If the certification step runs before lien checks complete, the buyer risks purchasing a property with hidden encumbrances. Missing a single lien can cost hundreds of thousands of dollars.

Without orchestration, title searches are manual and error-prone. A paralegal searches county records, another person checks for liens, and a title officer issues the certification. all coordinated via email. When the county records system is slow, the search stalls. When a lien check is missed, the title is certified incorrectly. Nobody can tell whether a search is in progress, stuck, or complete.

## The Solution

**You just write the record search, ownership verification, lien check, and title certification logic. Conductor handles lien search retries, ownership verification, and title audit trails.**

Each title search step is a simple, independent worker. one searches county records, one verifies current ownership, one checks for liens, one issues the certification. Conductor takes care of executing them in strict order so no certification is issued without a lien check, retrying if the county records system is temporarily unavailable, and maintaining a complete audit trail that title insurance underwriters can rely on.

### What You Write: Workers

Ownership history research, lien search, encumbrance analysis, and title report workers each investigate one aspect of property title status.

| Worker | Task | What It Does |
|---|---|---|
| **SearchRecordsWorker** | `tts_search` | Searches county recorder and assessor records for the property's chain of title |
| **VerifyOwnershipWorker** | `tts_verify_ownership` | Confirms current ownership by matching recorded deeds against the seller's identity |
| **CheckLiensWorker** | `tts_check_liens` | Searches for outstanding liens. tax, mechanic's, HOA, judgment, and federal liens |
| **CertifyTitleWorker** | `tts_certify` | Issues the title certification if ownership is verified and all liens are cleared |

Workers implement property transaction steps. listing, inspection, escrow, closing, with realistic outputs.

### The Workflow

```
tts_search
 │
 ▼
tts_verify_ownership
 │
 ▼
tts_check_liens
 │
 ▼
tts_certify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
