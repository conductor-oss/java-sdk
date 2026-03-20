# Premium Calculation in Java with Conductor :  Collect Rating Factors, Calculate Base, Apply Modifiers, Finalize

A Java Conductor workflow example for multi-step insurance premium calculation .  collecting rating factors (policy type, applicant age), computing the base premium from those factors and the coverage amount, applying discount and surcharge modifiers (good driver -10%, multi-policy -5%), and finalizing the adjusted premium. Each step builds on the previous: collected factors feed into the base rate calculation, the base premium and factors together determine which modifiers apply, and the adjusted premium is finalized into the quoted amount ($1,530/year from a $1,800 base). Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers ,  you write the business logic, Conductor handles retries, failure routing, durability, and observability for free.

## Premium Calculation Requires Sequential Factor Collection, Base Rating, and Modifier Application

Insurance premiums are not a single calculation .  they require gathering rating factors (age, location, coverage type), computing a base premium from actuarial rate tables, applying eligible discounts and surcharges, and finalizing the quoted amount. Each step depends on the previous: you cannot apply modifiers without knowing the base premium, and you cannot calculate the base without collecting the factors. If the modifier step fails, you need to retry it without recalculating the base premium.

## The Solution

**You just write the rating factor collection, base premium calculation, modifier application, and quote finalization logic. Conductor handles rate lookup retries, adjustment sequencing, and premium calculation audit trails.**

`CollectFactorsWorker` gathers all rating factors .  applicant demographics, property characteristics, coverage selections, deductible choices, and territory data. `CalculateBaseWorker` looks up the base premium from actuarial rate tables using the collected factors ,  applying filed rates for the state and policy type. `ApplyModifiersWorker` adjusts the base premium with applicable discounts and surcharges ,  multi-policy bundles, claims-free discounts, protective device credits, and experience surcharges. `FinalizeWorker` produces the final premium with regulatory fees, taxes, and payment plan options. Conductor records the full calculation chain for rate filing compliance and audit.

### What You Write: Workers

Risk factor collection, rate lookup, adjustment application, and quote generation workers each contribute one calculation layer to the final premium.

| Worker | Task | What It Does |
|---|---|---|
| **CollectFactorsWorker** | `pmc_collect_factors` | Collects rating factors for premium calculation .  gathers the policy type, applicant age, driving history, location, and other underwriting attributes needed for rate lookup |
| **CalculateBaseWorker** | `pmc_calculate_base` | Calculates the base premium .  applies actuarial rate tables to the collected factors and coverage amount to produce the base annual premium ($1,800/year) |
| **ApplyModifiersWorker** | `pmc_apply_modifiers` | Applies discount and surcharge modifiers to the base premium .  good driver discount (-10%), multi-policy bundle discount (-5%), and any applicable surcharges based on the rating factors |
| **FinalizeWorker** | `pmc_finalize` | Finalizes the premium quote .  rounds the adjusted premium to the final quoted amount ($1,530/year), applies any minimum premium floors or regulatory rate caps, and prepares the quote for presentation |

Workers simulate insurance operations .  claim intake, assessment, settlement ,  with realistic outputs. Replace with real claims management and underwriting integrations and the workflow stays the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically .  configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status .  no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

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

## Example Output

```
=== Example 708: Premium Calculatio ===

Step 1: Registering task definitions...
  Registered: pmc_collect_factors, pmc_calculate_base, pmc_apply_modifiers, pmc_finalize

Step 2: Registering workflow 'pmc_premium_calculation'...
  Workflow registered.

Step 3: Starting workers...
  4 workers polling.

Step 4: Starting workflow...
  Workflow ID: f7a2c1e9-...

  [modifiers] Good driver discount (-10%%), multi-policy (-5%%)
  [base] Base premium calculated: $1,800/year
  [collect_factors] Processing
  [finalize] Final premium: $1,530/year

  Status: COMPLETED
  Output: {adjustedPremium=..., discounts=..., basePremium=..., factors=...}

Result: PASSED
```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build
```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build
```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/premium-calculation-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/premium-calculation-1.0.0.jar --workers
```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow pmc_premium_calculation \
  --version 1 \
  --input '{"policyType": "standard", "auto": "sample-auto", "applicantAge": "sample-applicantAge", "coverageAmount": 250.0}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w pmc_premium_calculation -s COMPLETED -c 5
```

## How to Extend

Point each worker at your real rating systems .  your actuarial tables for base rates, your rating engine for discount and surcharge modifiers, your quoting platform for premium finalization, and the workflow runs identically in production.

- **CalculateBaseWorker** (`pmc_calculate_base`): integrate with actuarial rating engines (Earnix, Guidewire Rating) or implement ISO/AAIS rate table lookups for standard lines
- **CollectFactorsWorker** (`pmc_collect_factors`): query property data from CoreLogic, credit scores from TransUnion Insurance Bureau, and geo-risk data from Verisk for accurate territory rating
- **ApplyModifiersWorker** (`pmc_apply_modifiers`): implement tier-based pricing with discount stacking rules, experience modification factors, and regulatory cap enforcement per state

Update rating tables or risk factor weights and the calculation pipeline handles them without modification.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):


## Project Structure

```
premium-calculation-premium-calculation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/premiumcalculation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PremiumCalculationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyModifiersWorker.java
│       ├── CalculateBaseWorker.java
│       ├── CollectFactorsWorker.java
│       └── FinalizeWorker.java
└── src/test/java/premiumcalculation/workers/
    ├── CalculateBaseWorkerTest.java        # 1 tests
    └── FinalizeWorkerTest.java        # 1 tests
```
