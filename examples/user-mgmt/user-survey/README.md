# User Survey in Java Using Conductor : Creation, Distribution, Collection, Analysis, and Reporting

## The Problem

You need to gather structured feedback from users through surveys. That means creating a survey with a title and question set, sending it to a specific audience segment, collecting the responses, running analysis to extract satisfaction scores, recurring themes (ease of use, performance, pricing), and sentiment distribution (positive/neutral/negative), and finally producing a report that stakeholders can act on. Each step depends on the previous one. you can't distribute a survey before it's created, you can't analyze responses before they're collected, and the report needs both the survey ID and the analysis results.

Without orchestration, you'd build a monolithic survey service that handles creation, email blasts, response aggregation, and analysis in a single codebase. If distribution fails for some recipients, the entire pipeline stalls. If the analysis step takes longer than expected, there's no timeout protection. When you need to rerun just the report generation with updated analysis, you have to replay the whole flow manually.

## The Solution

**You just write the survey-creation, distribution, collection, analysis, and reporting workers. Conductor handles the survey lifecycle and response data flow.**

Each survey lifecycle phase. creation, distribution, collection, analysis, reporting, is a simple, independent worker. Conductor runs them in sequence, threads the generated survey ID from creation through every downstream step, feeds collected responses into the analyzer, and passes the analysis output into the report generator. If distribution times out or the analysis service fails, Conductor retries automatically and resumes from exactly where it left off.

### What You Write: Workers

CreateSurveyWorker generates a survey ID, DistributeSurveyWorker sends it to the audience, CollectResponsesWorker gathers answers, AnalyzeSurveyWorker computes satisfaction and sentiment, and SurveyReportWorker produces the summary.

| Worker | Task | What It Does |
|---|---|---|
| **CreateSurveyWorker** | `usv_create` | Creates a survey with a unique ID (SRV-XXXXXXXX), registers the title and question list, returns the question count |
| **DistributeSurveyWorker** | `usv_distribute` | Sends the survey to the target audience segment via email, in-app notification, or SMS |
| **CollectResponsesWorker** | `usv_collect` | Gathers submitted responses for the survey and returns the response set with a count |
| **AnalyzeSurveyWorker** | `usv_analyze` | Computes average satisfaction score, extracts top themes, and breaks down sentiment (positive/neutral/negative percentages) |
| **SurveyReportWorker** | `usv_report` | Generates a summary report from the analysis results, tied to the survey ID |

Replace with real identity provider and database calls and ### The Workflow

```
usv_create
 │
 ▼
usv_distribute
 │
 ▼
usv_collect
 │
 ▼
usv_analyze
 │
 ▼
usv_report

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
