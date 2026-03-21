# Chatbot Orchestration in Java with Conductor : Intent Detection and Response Generation Pipeline

## Turning a User Message into an Intelligent Response

A chatbot needs to do more than echo text back. Each incoming message must be received with its session context, analyzed for intent (is this a refund request? a cancellation? a general question?), used to generate a relevant response, and delivered back to the user's channel. These steps must happen in sequence. you cannot generate a response without knowing the intent, and you cannot deliver without a response.

This workflow models a single conversation turn. The receive step captures the message and loads session context. The intent classifier analyzes the message text, detecting intents like `request_refund`, `cancel_subscription`, or `general_inquiry` with a confidence score, and extracts entities (product name, dollar amount). The response generator uses the detected intent and entities to craft a reply. The delivery step sends the response back to the user's session.

## The Solution

**You just write the message-receiving, intent-detection, response-generation, and delivery workers. Conductor handles the conversation-turn pipeline.**

Four workers handle one chatbot turn. message receiving, intent understanding, response generation, and delivery. The intent worker scans for keywords like "refund" and "cancel" to classify the message and extract entities like product names and amounts. The response generator uses the classified intent and extracted entities to produce a relevant reply. Conductor sequences the four steps and tracks every conversation turn with full input/output visibility.

### What You Write: Workers

ReceiveWorker captures the message with session context, UnderstandIntentWorker classifies intent and extracts entities, GenerateResponseWorker crafts a reply, and DeliverWorker sends it back. One conversation turn, four independent steps.

| Worker | Task | What It Does |
|---|---|---|
| **DeliverWorker** | `cbo_deliver` | Sends the generated response back to the user's session/channel. |
| **GenerateResponseWorker** | `cbo_generate_response` | Crafts a contextual reply based on the detected intent and extracted entities. |
| **ReceiveWorker** | `cbo_receive` | Captures the incoming user message and loads session context. |
| **UnderstandIntentWorker** | `cbo_understand_intent` | Classifies the message intent (refund, cancellation, inquiry) and extracts entities (product name, amount). |

### The Workflow

```
cbo_receive
 │
 ▼
cbo_understand_intent
 │
 ▼
cbo_generate_response
 │
 ▼
cbo_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
