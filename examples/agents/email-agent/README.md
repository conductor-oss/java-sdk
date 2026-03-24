# Email Agent in Java Using Conductor : Analyze Intent, Draft, Review Tone, Send

Email Agent. analyze request, draft email, review tone, and send through a sequential pipeline.

## Writing the Right Email in the Right Tone

"Send a follow-up to Sarah about the Q4 proposal, keep it professional but warm." This requires understanding the intent (follow-up), identifying the context (Q4 proposal discussion), drafting content that addresses the topic, and then separately reviewing the tone. is it actually professional but warm, or did it come across as cold and transactional?

Tone review is a separate cognitive task from drafting. The same content can be delivered in drastically different tones, and getting the tone wrong (too casual in a formal context, too stiff in a friendly exchange) undermines the message. By separating drafting from tone review, you can iterate on tone without regenerating the entire email, and you can catch tone mismatches before sending.

## The Solution

**You write the intent analysis, drafting, tone review, and sending logic. Conductor handles the email pipeline, version tracking, and delivery confirmation.**

`AnalyzeRequestWorker` parses the user's intent (compose, reply, follow-up), extracts the recipient, context, and desired tone from the request. `DraftEmailWorker` generates the email body with subject line based on the analyzed intent and context. `ReviewToneWorker` evaluates the draft against the desired tone, scores the match, and adjusts wording where the tone deviates. `SendEmailWorker` delivers the tone-reviewed email to the recipient. Conductor chains these four steps and records the draft, tone adjustments, and final version for quality tracking.

### What You Write: Workers

Four workers handle the email workflow. Analyzing the request intent, drafting the content, reviewing tone against the desired style, and sending the message.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeRequestWorker** | `ea_analyze_request` | Analyzes the user's email intent and extracts structured information such as email type, key points, urgency, and for... |
| **DraftEmailWorker** | `ea_draft_email` | Drafts the email subject and body based on the analyzed request, key points, recipient, and desired tone. |
| **ReviewToneWorker** | `ea_review_tone` | Reviews the drafted email's tone against the desired tone. Returns a tone score, approval status, analysis breakdown,... |
| **SendEmailWorker** | `ea_send_email` | Simulates sending the approved email. Returns a fixed message ID, timestamp, and delivery status. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode. the agent workflow stays the same.

### The Workflow

```
ea_analyze_request
 │
 ▼
ea_draft_email
 │
 ▼
ea_review_tone
 │
 ▼
ea_send_email

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
