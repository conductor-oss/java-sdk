# Abandoned Cart Recovery in Java Using Conductor : Detect, Wait, Remind, Discount, Convert

Abandoned cart recovery: detect, wait, remind, offer discount, convert.

## 70% of Shopping Carts Are Abandoned

A customer adds $150 worth of items to their cart and leaves. Without intervention, that revenue is lost. Abandoned cart recovery sends a reminder email after a delay (typically 1-4 hours), and if the customer still hasn't returned, offers a discount to incentivize completion. This sequence recovers 5-15% of abandoned carts on average.

The recovery pipeline needs timed delays (wait 2 hours before sending the reminder), state tracking (did the customer return after the reminder?), and conditional logic (only offer a discount if the reminder didn't work). If the email service is temporarily unavailable, the reminder should be retried. not skipped. And every cart recovery attempt needs tracking to measure conversion rates and optimize timing.

## The Solution

**You just write the abandonment detection, reminder sending, discount offer, and conversion tracking logic. Conductor handles retry timing, recovery step sequencing, and conversion tracking across every cart.**

`DetectAbandonmentWorker` identifies the abandoned cart and captures cart details. items, total, customer ID, and abandonment timestamp. Conductor's `WAIT` task pauses the workflow for the configured delay period without consuming resources. `SendReminderWorker` sends a personalized email with the cart contents and a direct link to complete checkout. `OfferDiscountWorker` generates a time-limited discount code if the reminder hasn't driven conversion. `ConvertWorker` tracks whether the customer completed the purchase, recording the conversion method (reminder, discount, or organic return). Conductor handles the timing, retries failed email sends, and tracks conversion rates across all recovery attempts.

### What You Write: Workers

Workers for abandonment detection, reminder delivery, discount offers, and conversion tracking each focus on one recovery tactic without knowledge of the broader campaign.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertWorker** | `abc_convert` | Conversion attempt for cart |
| **DetectAbandonmentWorker** | `abc_detect_abandonment` | Detects the abandonment |
| **OfferDiscountWorker** | `abc_offer_discount` | Performs the offer discount operation |
| **SendReminderWorker** | `abc_send_reminder` | Performs the send reminder operation |

### The Workflow

```
abc_detect_abandonment
 │
 ▼
abc_wait_period [WAIT]
 │
 ▼
abc_send_reminder
 │
 ▼
abc_offer_discount
 │
 ▼
abc_convert

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
