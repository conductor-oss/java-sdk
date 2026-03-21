# AI Guardrails in Java Using Conductor : Input Check, Content Filter, Generate, Output Check, Deliver

## AI Models Need Safety Boundaries on Input and Output

An LLM without guardrails can be prompted to generate harmful content (instructions for dangerous activities), leak sensitive information (system prompts, training data), produce biased or discriminatory text, or violate content policies (explicit content, copyright infringement). Guardrails on input catch malicious prompts before they reach the model. Guardrails on output catch harmful responses before they reach the user.

Input guardrails detect prompt injection, jailbreak attempts, and policy-violating requests. Content filtering applies sensitivity rules (PII detection, topic restrictions). Output guardrails check for harmful content, factual claims that need verification, and compliance violations. Both sides need independent checking because a safe-looking prompt can produce harmful output, and a suspicious-looking prompt might produce perfectly safe output.

## The Solution

**You just write the input safety checking, content filtering, AI generation, output validation, and safe delivery logic. Conductor handles safety check sequencing, generation retries, and complete content audit trails.**

`InputCheckWorker` scans the user prompt for policy violations. jailbreak patterns, prompt injection attempts, restricted topics, and PII in the input. `ContentFilterWorker` applies sensitivity rules, topic restrictions, user-tier-based access controls, and content category filtering. `GenerateWorker` produces the AI response from the filtered prompt. `OutputCheckWorker` scans the generated response for harmful content, PII leakage, bias indicators, and compliance violations. `DeliverWorker` delivers the response only if both input and output checks pass. Conductor records every guardrail decision for safety auditing.

### What You Write: Workers

Safety checks run as independent workers before and after generation, so guardrail logic stays decoupled from the AI model itself.

| Worker | Task | What It Does |
|---|---|---|
| **ContentFilterWorker** | `grl_content_filter` | Applies content sensitivity rules. checks for harmful topics, restricted categories, and policy violations |
| **DeliverWorker** | `grl_deliver` | Delivers the safe, validated response to the user after all guardrail checks pass |
| **GenerateWorker** | `grl_generate` | Generates the AI response from the filtered prompt using the specified model |
| **InputCheckWorker** | `grl_input_check` | Scans the user prompt for safety. checks for PII, prompt injection attempts, and policy violations |
| **OutputCheckWorker** | `grl_output_check` | Validates the generated response. checks toxicity (0.01), hallucination (0.03), and content safety |

Workers implement AI generation stages with realistic outputs so you can see the pipeline without API keys. Set the provider API key to switch to live mode. the generation workflow stays the same.

### The Workflow

```
grl_input_check
 │
 ▼
grl_content_filter
 │
 ▼
grl_generate
 │
 ▼
grl_output_check
 │
 ▼
grl_deliver

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
