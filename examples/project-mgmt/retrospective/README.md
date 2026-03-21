# Sprint Retrospective in Java with Conductor : Feedback Collection, Categorization, Prioritization, and Action Items

## The Problem

You need to run retrospectives consistently across sprints and teams. After each sprint, the team provides feedback. blockers they hit, processes that worked, tools that slowed them down. That feedback needs to be collected from multiple sources (survey forms, Slack threads, meeting notes), categorized into themes (process, tooling, communication, technical debt), prioritized by how many people raised the issue and its impact on velocity, and turned into concrete action items assigned to owners with deadlines.

Without orchestration, retrospectives become ad-hoc meetings where feedback is captured in a Google Doc, never categorized, and the same issues surface sprint after sprint. Building this as a script means a failure pulling feedback from Slack silently skips categorization, and you lose the ability to track which action items were actually generated and whether they carried over from previous sprints.

## The Solution

**You just write the feedback collection, theme categorization, impact prioritization, and action item generation logic. Conductor handles feedback collection retries, theme analysis, and action item tracking.**

Each retrospective step is a simple, independent worker. one collects raw feedback, one categorizes it into themes, one prioritizes by frequency and impact, one generates action items with owners and deadlines. Conductor takes care of executing them in sequence, retrying if a data source is temporarily unavailable, and maintaining a historical record of every retrospective so you can track improvement trends across sprints. ### What You Write: Workers

Feedback collection, theme identification, action item creation, and follow-up tracking workers each own one step of the team retrospective process.

| Worker | Task | What It Does |
|---|---|---|
| **CollectFeedbackWorker** | `rsp_collect_feedback` | Gathers team feedback from surveys, Slack channels, and meeting notes for the given sprint |
| **CategorizeWorker** | `rsp_categorize` | Groups raw feedback into themes. process, tooling, communication, technical debt, team dynamics |
| **PrioritizeWorker** | `rsp_prioritize` | Ranks categorized items by frequency, impact on velocity, and team sentiment scores |
| **ActionItemsWorker** | `rsp_action_items` | Converts top priorities into concrete action items with owners, deadlines, and success criteria |

### The Workflow

```
rsp_collect_feedback
 │
 ▼
rsp_categorize
 │
 ▼
rsp_prioritize
 │
 ▼
rsp_action_items

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
