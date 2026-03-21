# Lead Scoring in Java with Conductor: Collect Signals, Score, Classify, and Route Leads to Sales

Your top rep just spent three weeks nurturing a lead who was never going to buy: meanwhile, a VP of Engineering who visited your pricing page four times and downloaded your security whitepaper sat untouched in the queue until a competitor closed them. This happens constantly when every lead looks the same in the CRM: salespeople guess who to call based on gut feel, hot leads cool off waiting for attention, and cold leads waste hours of expensive human outreach. This workflow scores leads automatically, collecting behavioral signals, computing a numeric score, classifying urgency, and routing to the right rep, so your team works the leads that are actually ready to buy. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the four-step scoring pipeline.

## Focusing Sales on the Leads Most Likely to Convert

Sales teams cannot pursue every lead equally. A lead who visited the pricing page three times and downloaded a whitepaper is more likely to convert than one who opened a single marketing email. Lead scoring quantifies buying intent by aggregating behavioral signals into a numeric score, classifying the lead by urgency, and routing hot leads to senior reps while cold leads go to automated nurturing.

This workflow processes one lead through the scoring pipeline. The signal collector gathers behavioral data. Page visits, email opens, content downloads, demo requests. The scorer weighs those signals and computes a numeric score. The classifier maps the score to a category: hot (high score, ready to buy), warm (moderate engagement, needs nurturing), or cold (low engagement, not ready). The router assigns the lead to the right sales rep or automation track based on the classification.

## The Solution

**You just write the signal-collection, scoring, classification, and routing workers. Conductor handles the scoring pipeline and lead data flow.**

Four workers form the scoring pipeline. Signal collection, scoring, classification, and routing. The signal collector gathers engagement data across channels. The scorer computes a weighted score from those signals. The classifier labels the lead as hot, warm, or cold. The router assigns the lead to a sales rep or nurture track. Conductor sequences the four steps and passes signals, scores, and classifications between them via JSONPath.

### What You Write: Workers

CollectSignalsWorker gathers page visits and email engagement, ScoreWorker computes a weighted score, ClassifyWorker labels the lead as hot/warm/cold, and RouteWorker assigns them to a sales rep or nurture track.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyWorker** | `ls_classify` | Classifies a lead based on score into hot/warm/cold. |
| **CollectSignalsWorker** | `ls_collect_signals` | Collects behavioral signals for a lead. |
| **RouteWorker** | `ls_route` | Routes a lead to the appropriate sales rep based on classification. |
| **ScoreWorker** | `ls_score` | Calculates lead score from collected signals. |

### The Workflow

```
ls_collect_signals
 │
 ▼
ls_score
 │
 ▼
ls_classify
 │
 ▼
ls_route

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
