# Recurring Billing in Java Using Conductor : Scheduled Invoice Generation, Payment Processing, and Notification

## The Problem

You need to bill customers on a recurring basis. monthly subscriptions, annual renewals, usage-based charges. Each billing cycle must generate an invoice, attempt to charge the customer's saved payment method, handle payment failures (retry, update card prompt), and send a receipt or failure notification. If the invoice generation step fails, you can't charge. If the charge fails, you need to retry before suspending service.

Without orchestration, recurring billing is a cron job that queries subscribers, charges each one in a loop, and sends emails. When the payment gateway times out for one customer, the entire batch stalls. Failed charges aren't retried systematically, and there's no audit trail connecting invoices to payments to notifications.

## The Solution

**You just write the invoice generation and payment gateway integration. Conductor handles the generate-charge-notify sequence, automatic retries on payment gateway failures, and a complete audit trail linking every invoice to its payment attempt and customer notification.**

Each billing concern is an independent worker. invoice generation, payment processing, and notification. Conductor runs them in sequence: generate the invoice, charge the payment method, then notify the customer. Failed payments are retried automatically with Conductor's retry logic. Every billing cycle is tracked with invoice details, payment status, and notification delivery. ### What You Write: Workers

This workflow uses Conductor system tasks to generate invoices, process payments against saved methods, and send customer notifications, each billing step handled by built-in task types.

This example uses Conductor system tasks. no custom workers needed.

The workflow relies on Conductor's built-in task types. To go to production, add custom workers as needed. the worker interface is simple, and no workflow changes are required.

### The Workflow

```
Input -> -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
