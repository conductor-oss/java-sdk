# Tax Calculation in Java Using Conductor : Determine Jurisdiction, Calculate Rates, Apply, Report

## Sales Tax Is Surprisingly Complex

A $100 order shipping to Boulder, Colorado has four layers of sales tax: Colorado state (2.9%), Boulder County (0.985%), City of Boulder (3.86%), and RTD special district (1.0%). totaling 8.745%, or $8.75 in tax. Ship the same order to Portland, Oregon, and the tax is $0 (Oregon has no sales tax). Ship it to Chicago, and the rate depends on the product category, clothing, food, and electronics have different rates.

Tax jurisdiction determination requires mapping the shipping address to the correct taxing authorities. which is not simply the state. There are over 13,000 tax jurisdictions in the US alone, with rates that change quarterly. Product category matters: some jurisdictions exempt food, clothing under $110, or digital goods. Getting this wrong means either overcharging customers (trust issue) or underpaying tax authorities (compliance risk and penalties).

## The Solution

**You just write the jurisdiction resolution, rate lookup, tax computation, and compliance reporting logic. Conductor handles jurisdiction lookups, rate retries, and tax calculation audit trails for every transaction.**

`DetermineJurisdictionWorker` maps the shipping address to all applicable tax jurisdictions. state, county, city, and special districts, using geocoding or ZIP+4 lookup. `CalculateRatesWorker` looks up the current tax rate for each jurisdiction, applying product category exemptions and thresholds. `ApplyWorker` computes the total tax by applying all jurisdiction rates to the eligible order amount, handling rounding rules per jurisdiction. `ReportWorker` generates a tax compliance report showing the tax breakdown by jurisdiction for filing with tax authorities. Conductor records every tax calculation for audit and compliance.

### What You Write: Workers

Address validation, jurisdiction lookup, rate application, and exemption workers each solve one piece of the tax determination puzzle.

| Worker | Task | What It Does |
|---|---|---|
| **DetermineJurisdictionWorker** | `tax_determine_jurisdiction` | Resolves the shipping address to a tax jurisdiction (state, county, city, district) |
| **CalculateRatesWorker** | `tax_calculate_rates` | Looks up the combined tax rate for the resolved jurisdiction |
| **ApplyTaxWorker** | `tax_apply` | Computes the tax amount by applying the rate to the order subtotal |
| **TaxReportWorker** | `tax_report` | Records the tax transaction for compliance and filing purposes |

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
