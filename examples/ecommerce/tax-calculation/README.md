# Tax Calculation in Java Using Conductor :  Determine Jurisdiction, Calculate Rates, Apply, Report

A Java Conductor workflow example for sales tax calculation .  resolving the tax jurisdiction from a shipping address, computing the applicable federal/state/local/district tax rates, applying the combined rate to the order subtotal, and recording the tax obligation for compliance reporting. Uses [Conductor](https://github.

## Sales Tax Is Surprisingly Complex

A $100 order shipping to Boulder, Colorado has four layers of sales tax: Colorado state (2.9%), Boulder County (0.985%), City of Boulder (3.86%), and RTD special district (1.0%) .  totaling 8.745%, or $8.75 in tax. Ship the same order to Portland, Oregon, and the tax is $0 (Oregon has no sales tax). Ship it to Chicago, and the rate depends on the product category ,  clothing, food, and electronics have different rates.

Tax jurisdiction determination requires mapping the shipping address to the correct taxing authorities .  which is not simply the state. There are over 13,000 tax jurisdictions in the US alone, with rates that change quarterly. Product category matters: some jurisdictions exempt food, clothing under $110, or digital goods. Getting this wrong means either overcharging customers (trust issue) or underpaying tax authorities (compliance risk and penalties).

## The Solution

**You just write the jurisdiction resolution, rate lookup, tax computation, and compliance reporting logic. Conductor handles jurisdiction lookups, rate retries, and tax calculation audit trails for every transaction.**

`DetermineJurisdictionWorker` maps the shipping address to all applicable tax jurisdictions .  state, county, city, and special districts ,  using geocoding or ZIP+4 lookup. `CalculateRatesWorker` looks up the current tax rate for each jurisdiction, applying product category exemptions and thresholds. `ApplyWorker` computes the total tax by applying all jurisdiction rates to the eligible order amount, handling rounding rules per jurisdiction. `ReportWorker` generates a tax compliance report showing the tax breakdown by jurisdiction for filing with tax authorities. Conductor records every tax calculation for audit and compliance.

### What You Write: Workers

Address validation, jurisdiction lookup, rate application, and exemption workers each solve one piece of the tax determination puzzle.

| Worker | Task | What It Does |
|---|---|---|
| **DetermineJurisdictionWorker** | `tax_determine_jurisdiction` | Resolves the shipping address to a tax jurisdiction (state, county, city, district) |
| **CalculateRatesWorker** | `tax_calculate_rates` | Looks up the combined tax rate for the resolved jurisdiction |
| **ApplyTaxWorker** | `tax_apply` | Computes the tax amount by applying the rate to the order subtotal |
| **TaxReportWorker** | `tax_report` | Records the tax transaction for compliance and filing purposes |

Workers simulate e-commerce operations .  payment processing, inventory checks, shipping ,  with realistic outputs so you can run the full order flow. Replace with real service integrations and the workflow stays the same.

### The Workflow

```
tax_determine_jurisdiction
    │
    ▼
tax_calculate_rates
    │
    ▼
tax_apply
    │
    ▼
tax_report

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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/tax-calculation-1.0.0.jar

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
java -jar target/tax-calculation-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow tax_calculation_workflow \
  --version 1 \
  --input '{"orderId": "TEST-001", "subtotal": "sample-subtotal", "shippingAddress": "sample-shippingAddress"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w tax_calculation_workflow -s COMPLETED -c 5

```

## How to Extend

Swap each worker for real tax services. Avalara AvaTax or TaxJar for jurisdiction lookup and rate calculation, your ERP for compliance reporting, and the workflow runs identically in production.

- **DetermineJurisdictionWorker** (`tax_determine_jurisdiction`): use Avalara AvaTax, TaxJar, or Vertex for accurate jurisdiction determination that handles edge cases (ZIP codes spanning multiple jurisdictions, tribal lands, military bases)
- **CalculateRatesWorker** (`tax_calculate_rates`): integrate with tax rate databases that update quarterly, or use Avalara/TaxJar APIs for real-time rates with product taxability rules per jurisdiction
- **ReportWorker** (`tax_report`): generate filing-ready reports for each state, integrate with Avalara Returns or TaxJar AutoFile for automated sales tax return filing

Switch tax providers or add new jurisdiction rules and the calculation pipeline handles them seamlessly.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

## Project Structure

```
tax-calculation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/taxcalculation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── TaxCalculationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ApplyTaxWorker.java
│       ├── CalculateRatesWorker.java
│       ├── DetermineJurisdictionWorker.java
│       └── TaxReportWorker.java
└── src/test/java/taxcalculation/workers/
    ├── ApplyTaxWorkerTest.java        # 2 tests
    └── CalculateRatesWorkerTest.java        # 2 tests

```
