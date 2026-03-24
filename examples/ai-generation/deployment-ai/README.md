# Deployment AI: Risk-Aware Deployment with Strategy Recommendation

A team is about to deploy a release that includes a database migration and two API changes. Deploying this as a simple rolling update could cause data loss if the migration fails or break clients if the API changes are incompatible. The deployment strategy should be driven by the actual risk profile of the changes, not by a one-size-fits-all policy.

This workflow analyzes the changes, predicts risk, recommends an appropriate deployment strategy, and executes the deployment.

## Pipeline Architecture

```
serviceName, version, environment
         |
         v
  dai_analyze_changes    (changeAnalysis map, changeCount)
         |
         v
  dai_predict_risk       (riskLevel, factors=["db_migration", "api_changes"])
         |
         v
  dai_recommend_strategy (strategy based on risk level)
         |
         v
  dai_execute_deploy     (deployStatus="success", strategy used)
```

## Worker: AnalyzeChanges (`dai_analyze_changes`)

Examines the release to produce a `changeAnalysis` map with fields like `filesChanged: 8`, `dbMigrations: 1`, and `apiChanges: 2`. Returns `changeCount` equal to `filesChanged` from the analysis. The presence of database migrations and API changes are the primary risk signals.

## Worker: PredictRisk (`dai_predict_risk`)

Evaluates the change analysis to predict deployment risk. Returns `riskLevel` (high, medium, or low) and `factors: ["db_migration", "api_changes"]` -- the specific elements that contribute to the risk assessment. The confidence in the prediction is `0.87`. A high risk level triggers a more conservative deployment strategy.

## Worker: RecommendStrategy (`dai_recommend_strategy`)

Maps risk level to deployment strategy using a decision table: `Map.of("high", "canary", "medium", "blue-green", "low", "rolling")`. High-risk changes get canary deployments (gradual traffic shift), medium risk gets blue-green (instant cutover with rollback), and low risk gets rolling updates. Returns the selected `strategy`.

## Worker: ExecuteDeploy (`dai_execute_deploy`)

Executes the deployment using the recommended strategy. Returns `deployStatus: "success"` and the `strategy` that was applied. The strategy determines the actual deployment mechanics (canary percentage, blue-green switch, rolling batch size).

## Tests

4 tests cover change analysis, risk prediction, strategy recommendation, and deployment execution.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
