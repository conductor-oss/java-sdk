# Multi-Agent Customer Support: Classify, Route, Resolve, Validate

A support system receives tickets with a subject, description, and customer tier. Different ticket types (bugs, feature requests, general inquiries) require fundamentally different handling paths -- a bug needs knowledge base lookup and a fix proposal, a feature request needs roadmap evaluation, and a general question just needs documentation links. Routing everything through a single handler wastes time and produces generic answers.

This workflow classifies the ticket, routes it through a SWITCH task to the appropriate specialist branch, then runs a QA validation pass on every response before delivery.

## Pipeline Architecture

```
ticketId, subject, description, customerTier
         |
         v
  cs_classify_ticket        (category, severity, keywords, confidence=0.94)
         |
         v
  SWITCH on category ----+------------------+
    |                    |                  |
   bug                 feature           default
    |                    |                  |
  cs_knowledge_search  cs_feature_evaluate  cs_general_respond
    |
  cs_solution_propose
    |                    |                  |
    +----------+---------+------------------+
               |
               v
  cs_qa_validate            (approved, checks, finalResponse)
```

## Worker: ClassifyTicket (`cs_classify_ticket`)

Concatenates subject and description, lowercases, and scans for keyword sets. Bug keywords: `error`, `crash`, `bug`, `broken`, `fail` (severity=`"high"`). Feature keywords: `feature`, `request`, `enhance`, `add` (severity=`"medium"`). Anything else maps to `general` with severity `"low"`. Each matched keyword is collected into an `ArrayList<String>`. Returns a fixed `confidence` of `0.94`.

## Worker: KnowledgeSearch (`cs_knowledge_search`)

Returns three hardcoded KB articles as `List<Map<String, Object>>` with fields `id`, `title`, `relevance`, and `excerpt`. Relevance scores are `0.95`, `0.87`, and `0.82` for articles KB-1001 through KB-1003. Reports `searchTime` as `"120ms"`.

## Worker: SolutionPropose (`cs_solution_propose`)

Produces a four-step fix response (clear cache, verify config, check resources, apply patch) referencing KB articles `["KB-1001", "KB-1002", "KB-1003"]`. Tags the solution as `solutionType: "known_fix"`.

## Worker: FeatureEvaluate (`cs_feature_evaluate`)

Evaluates a feature request considering the `customerTier`. Returns fixed priority `"high"`, ETA `"Q2 2026"`, and roadmap tracking ID `"FR-4521"`.

## Worker: GeneralRespond (`cs_general_respond`)

Returns a generic response with `suggestedDocs`: `["Getting Started Guide", "FAQ - Frequently Asked Questions", "API Documentation"]`.

## Worker: QaValidate (`cs_qa_validate`)

Runs four boolean checks stored in a `LinkedHashMap`: `toneAppropriate`, `includesGreeting`, `includesNextSteps`, and `noSensitiveData` -- all set to `true`. Assembles a `finalResponse` string with the ticket ID and category embedded. Returns `approved: true` and `reviewedAt: "2026-03-14T10:30:00Z"`.

## Tests

6 tests cover classification, all three routing branches, solution proposal, and QA validation.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
