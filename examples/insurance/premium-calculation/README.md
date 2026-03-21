# Premium Calculation in Java with Conductor : Collect Rating Factors, Calculate Base, Apply Modifiers, Finalize

## Premium Calculation Requires Sequential Factor Collection, Base Rating, and Modifier Application

Insurance premiums are not a single calculation. they require gathering rating factors (age, location, coverage type), computing a base premium from actuarial rate tables, applying eligible discounts and surcharges, and finalizing the quoted amount. Each step depends on the previous: you cannot apply modifiers without knowing the base premium, and you cannot calculate the base without collecting the factors. If the modifier step fails, you need to retry it without recalculating the base premium.

## The Solution

**You just write the rating factor collection, base premium calculation, modifier application, and quote finalization logic. Conductor handles rate lookup retries, adjustment sequencing, and premium calculation audit trails.**

`CollectFactorsWorker` gathers all rating factors. applicant demographics, property characteristics, coverage selections, deductible choices, and territory data. `CalculateBaseWorker` looks up the base premium from actuarial rate tables using the collected factors, applying filed rates for the state and policy type. `ApplyModifiersWorker` adjusts the base premium with applicable discounts and surcharges, multi-policy bundles, claims-free discounts, protective device credits, and experience surcharges. `FinalizeWorker` produces the final premium with regulatory fees, taxes, and payment plan options. Conductor records the full calculation chain for rate filing compliance and audit.

### What You Write: Workers

Risk factor collection, rate lookup, adjustment application, and quote generation workers each contribute one calculation layer to the final premium.

| Worker | Task | What It Does |
|---|---|---|
| **CollectFactorsWorker** | `pmc_collect_factors` | Collects rating factors for premium calculation. gathers the policy type, applicant age, driving history, location, and other underwriting attributes needed for rate lookup |
| **CalculateBaseWorker** | `pmc_calculate_base` | Calculates the base premium. applies actuarial rate tables to the collected factors and coverage amount to produce the base annual premium ($1,800/year) |
| **ApplyModifiersWorker** | `pmc_apply_modifiers` | Applies discount and surcharge modifiers to the base premium. good driver discount (-10%), multi-policy bundle discount (-5%), and any applicable surcharges based on the rating factors |
| **FinalizeWorker** | `pmc_finalize` | Finalizes the premium quote. rounds the adjusted premium to the final quoted amount ($1,530/year), applies any minimum premium floors or regulatory rate caps, and prepares the quote for presentation |

### The Workflow

```
pmc_collect_factors
 │
 ▼
pmc_calculate_base
 │
 ▼
pmc_apply_modifiers
 │
 ▼
pmc_finalize

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
