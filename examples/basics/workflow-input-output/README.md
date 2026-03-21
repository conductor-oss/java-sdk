# Workflow Input/Output in Java with Conductor: Data Flow via JSONPath in a Price Calculator

Your workflow runs, all tasks complete, but the output is empty, or worse, silently wrong. You passed `{"orderId": "123"}` but the task expected `${workflow.input.order_id}` with an underscore, not camelCase. Data flow in Conductor has rules: JSONPath expressions like `${taskRef.output.field}` wire each task's output to the next task's input, and a single typo means a null that propagates silently through the entire pipeline. This example makes those rules visible with a four-step price calculator (lookup, discount, tax, invoice) where you can trace every value from workflow input through each JSONPath expression to the final formatted output.

## Understanding Data Flow Between Tasks

The most important concept in Conductor workflows is how data flows: workflow inputs feed into the first task, each task's output can be referenced by subsequent tasks using JSONPath expressions, and the workflow output assembles results from any task in the pipeline. This price calculator makes the data flow concrete and traceable.

Starting with `productId`, `quantity`, and `couponCode` as workflow input: the price lookup produces a `unitPrice`, the discount applier reads that price and the coupon to produce a `discountedPrice`, the tax calculator reads the discounted price to produce a `taxAmount` and `total`, and the invoice formatter assembles everything into a formatted output.

## The Solution

**You just write the price lookup, discount application, tax calculation, and invoice formatting logic. Conductor handles the data flow between them via JSONPath.**

Four workers form the price calculation pipeline. Price lookup, discount application, tax calculation, and invoice formatting. The workflow JSON declares how each task reads from previous tasks' outputs using JSONPath, making the data flow explicit and inspectable in the Conductor UI.

### What You Write: Workers

The price calculator workers show how JSONPath expressions route specific fields from workflow input to each worker and aggregate their outputs.

| Worker | Task | What It Does |
|---|---|---|
| **LookupPriceWorker** | `lookup_price` | Resolves a product ID against a built-in catalog (PROD-001 = Wireless Keyboard $79.99, PROD-002 = 27" Monitor $349.99, PROD-003 = USB-C Hub $49.99) and multiplies by quantity to produce a subtotal. |
| **ApplyDiscountWorker** | `apply_discount` | Looks up a coupon code (SAVE10 = 10%, SAVE20 = 20%, HALF = 50%) and subtracts the discount from the subtotal. Unknown or missing coupons apply 0% discount. |
| **CalculateTaxWorker** | `calculate_tax` | Applies a fixed 8.25% sales tax rate to the discounted total and returns the tax amount and final total. |
| **FormatInvoiceWorker** | `format_invoice` | Assembles all prior outputs (product, price, discount, tax) into a formatted plaintext invoice string. |

### The Workflow

```
lookup_price
 │
 ▼
apply_discount
 │
 ▼
calculate_tax
 │
 ▼
format_invoice

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
