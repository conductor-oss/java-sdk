# Risk-Aware Deployment: Analyze Changes, Predict Risk, Recommend Strategy

Before deploying, the AI analyzes changes (`{filesChanged: 8, dbMigrations: 1, apiChanges: 2}`), predicts risk based on factors like `["db_migration", "api_changes"]` with confidence 0.87, recommends a strategy from a `Map.of("high", "canary", "medium", "blue-green", "low", "rolling")`, and executes the deployment.

## Workflow

```
serviceName, version, environment
  -> dai_analyze_changes -> dai_predict_risk -> dai_recommend_strategy -> dai_execute_deploy
```

## Tests

8 tests cover change analysis, risk prediction, strategy recommendation, and deployment.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
