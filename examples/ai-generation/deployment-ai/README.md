# Deployment AI in Java with Conductor : Risk-Aware Deployment with Change Analysis and Strategy Selection

## Deploying with Confidence Instead of Guesswork

Every deployment carries risk. A version with 200 changed files across critical modules needs a different deployment strategy than a version with 3 changed config lines. Teams often pick a strategy based on gut feeling or always use the same approach regardless of risk. This leads to either over-cautious deployments (canary for trivial changes, wasting time) or reckless ones (direct push for risky changes, causing outages).

This workflow makes deployment strategy data-driven. The change analyzer examines what changed between versions. file count, modules affected, breaking changes. The risk predictor evaluates the change analysis and assigns a risk level. The strategy recommender selects the appropriate deployment method based on risk and target environment (production gets more cautious strategies than staging). The deploy executor carries out the chosen strategy. Each step's output informs the next, risk levels drive strategy selection, and strategy drives execution parameters.

## The Solution

**You just write the change-analysis, risk-prediction, strategy-recommendation, and deployment workers. Conductor handles the decision chain and execution sequencing.**

Four workers form the deployment pipeline. change analysis, risk prediction, strategy recommendation, and deployment execution. The change analyzer examines the diff between versions. The risk predictor scores the changes. The strategy recommender maps risk level and environment to a deployment approach. The executor deploys using the recommended strategy. Conductor sequences the decision chain and ensures the deployment strategy is always backed by data.

### What You Write: Workers

AnalyzeChangesWorker examines the diff between versions, PredictRiskWorker scores deployment risk, RecommendStrategyWorker selects blue-green/canary/rolling, and ExecuteDeployWorker carries out the deployment.

| Worker | Task | What It Does |
|---|---|---|
| **AnalyzeChangesWorker** | `dai_analyze_changes` | Examines the diff between versions: file count, affected modules, database migrations, and breaking changes. |
| **ExecuteDeployWorker** | `dai_execute_deploy` | Deploys the service version using the recommended strategy (blue-green, canary, rolling, or direct). |
| **PredictRiskWorker** | `dai_predict_risk` | Scores deployment risk based on the change analysis (low, medium, high, critical). |
| **RecommendStrategyWorker** | `dai_recommend_strategy` | Selects the deployment strategy (blue-green, canary, rolling, direct) based on risk level and target environment. |

### The Workflow

```
dai_analyze_changes
 │
 ▼
dai_predict_risk
 │
 ▼
dai_recommend_strategy
 │
 ▼
dai_execute_deploy

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
