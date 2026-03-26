# Routing a Chat Message from Intent Detection to Response Delivery

A customer types "I want a refund for the Pro Plan" into a webchat. Before a response can be generated, the system must capture the message with its session context, classify the intent, extract relevant entities (product name, dollar amount), generate an intent-specific reply, and deliver it back on the correct channel. All four steps must execute in strict order.

## Workflow

```
userId, message, sessionId
       │
       ▼
┌─────────────┐
│ cbo_receive  │  Capture message + load session context
└──────┬──────┘
       │  context {previousTurns: 3, topic: "billing"}
       ▼
┌────────────────────────┐
│ cbo_understand_intent  │  Classify intent + extract entities
└──────────┬─────────────┘
           │  intent, confidence, entities
           ▼
┌────────────────────────┐
│ cbo_generate_response  │  Craft intent-specific reply
└──────────┬─────────────┘
           │  response, model, tokensUsed
           ▼
┌──────────────┐
│ cbo_deliver  │  Send to webchat channel
└──────────────┘
       │
       ▼
  intent, response, delivered
```

## Workers

**ReceiveWorker** (`cbo_receive`) -- Logs the incoming message with its `userId`, then returns a context map with `previousTurns: 3` and `topic: "billing"`. Stamps `receivedAt` with `Instant.now().toString()`.

**UnderstandIntentWorker** (`cbo_understand_intent`) -- Lowercases the message and applies keyword matching: `msg.contains("refund")` maps to `"request_refund"`, `msg.contains("cancel")` maps to `"cancel_subscription"`, everything else falls through to `"general_inquiry"`. Always reports a confidence of `0.94`. Extracts hardcoded entities: `product: "Pro Plan"`, `amount: "$49.99"`.

**GenerateResponseWorker** (`cbo_generate_response`) -- Selects from a `Map.of()` of three canned responses keyed by intent. The refund response mentions the Pro Plan and $49.99 amount. The cancellation response offers to walk through the process. The general inquiry response asks for more detail. Reports `model: "gpt-4"` and `tokensUsed: 85`.

**DeliverWorker** (`cbo_deliver`) -- Marks the response as delivered (`delivered: true`) on the `"webchat"` channel with a `latencyMs` of 320.

## Tests

8 tests across 4 test files verify intent classification logic, response selection, and delivery confirmation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
