# Email Agent: Analyze Intent, Draft, Review Tone, Send

The agent analyzes an email request to detect the type (`"project_update"`) and extract key points, drafts the email with subject line, reviews the tone (producing a score, approval status, and suggestions), and sends it (returning `messageId: "msg-fixed-abc123"`).

## Workflow

```
intent, recipient, context, desiredTone
  -> ea_analyze_request -> ea_draft_email -> ea_review_tone -> ea_send_email
```

## Workers

**AnalyzeRequestWorker** (`ea_analyze_request`) -- Returns `emailType: "project_update"` and key points.

**DraftEmailWorker** (`ea_draft_email`) -- Produces subject and body from key points.

**ReviewToneWorker** (`ea_review_tone`) -- Returns tone score, approval, analysis breakdown, and suggestions.

**SendEmailWorker** (`ea_send_email`) -- Returns `sent: true`, `messageId: "msg-fixed-abc123"`.

## Tests

33 tests cover intent analysis, drafting, tone review, and sending.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
