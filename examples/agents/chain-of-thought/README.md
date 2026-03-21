# Chain-of-Thought in Java Using Conductor: Understand, Reason, Calculate, Verify, Answer

You ask the model "What's the compound interest on $10,000 at 5% for 3 years?" and it confidently replies "$11,576.25." Wrong. The real answer is $11,576.25 only if compounded annually; but you didn't specify, and the model didn't ask. Worse, you can't tell where it went wrong because the entire reasoning happened inside a single opaque API call. Did it misunderstand the problem, pick the wrong formula, or botch the arithmetic? This example externalizes each reasoning step: understand, reason, calculate, verify, answer, as a separate Conductor worker, so you can see exactly which step failed and retry just that step. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers, you write the business logic, Conductor handles retries, failure routing, durability, and observability.

## Making AI Reasoning Transparent and Debuggable

When an LLM answers a complex question in a single call, you can't see where it went wrong. Did it misunderstand the problem? Was its approach correct but the calculation wrong? Did it skip the verification step? Chain-of-thought prompting helps, but the reasoning is still a single opaque text blob.

Externalizing each reasoning step as a separate worker makes the thought process observable. You can see that the model understood the problem correctly (step 1), chose a valid approach (step 2), but made an arithmetic error (step 3) that the verification step caught (step 4). If the calculation step fails or produces an incorrect result, you can retry just that step with the understanding and reasoning context preserved.

## The Solution

**You write the understanding, reasoning, calculation, and verification logic. Conductor handles the reasoning chain, step-level retries, and full observability into each thought.**

`UnderstandProblemWorker` parses the problem statement and extracts the key question, known values, and what needs to be found. `Step1ReasonWorker` develops the solution approach, which formulas to use, what assumptions to make, and the logical sequence. `Step2CalculateWorker` executes the calculations step by step using the chosen approach. `Step3VerifyWorker` checks the result by substituting back into the original constraints or using an alternative method. `FinalAnswerWorker` assembles the verified result with the full reasoning chain as the final answer. Conductor makes each reasoning step individually observable and retriable.

### What You Write: Workers

Five workers externalize the reasoning chain. Understanding the problem, choosing an approach, calculating step by step, verifying the result, and assembling the final answer.

| Worker | Task | What It Does |
|---|---|---|
| **FinalAnswerWorker** | `ct_final_answer` | Produces the final human-readable answer combining the verified result and confidence score. |
| **Step1ReasonWorker** | `ct_step_1_reason` | Takes the problem understanding and produces a reasoning step identifying the formula and variables to use. |
| **Step2CalculateWorker** | `ct_step_2_calculate` | Step2s Calculate and computes calculation, interest |
| **Step3VerifyWorker** | `ct_step_3_verify` | Verifies the calculation result using fixed (hardcoded) verification values. No year-by-year computation is performed. |
| **UnderstandProblemWorker** | `ct_understand_problem` | Analyzes the incoming problem and produces a structured understanding including the problem type and known values. |

Workers implement agent decisions and tool calls with realistic outputs so you can see the routing and handoff patterns without live LLM calls. Add your API keys to switch to live mode, the agent workflow stays the same.

### The Workflow

```
ct_understand_problem
 │
 ▼
ct_step_1_reason
 │
 ▼
ct_step_2_calculate
 │
 ▼
ct_step_3_verify
 │
 ▼
ct_final_answer

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
