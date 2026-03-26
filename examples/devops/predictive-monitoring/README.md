# Predicting CPU Breaches Before They Happen

Reactive alerting tells you the CPU is at 95% right now. By then it is too late -- latency
is already spiking. This workflow collects 30 days of metric history (43,200 data points at
1-minute granularity), trains a Prophet model, predicts the next N hours, and fires an alert
if a breach is likely.

## Workflow

```
metricName, historyDays, forecastHours
                |
                v
+----------------------+     +-------------------+     +--------------+     +---------------+
| pdm_collect_history  | --> | pdm_train_model   | --> | pdm_predict  | --> | pdm_alert     |
+----------------------+     +-------------------+     +--------------+     +---------------+
  43200 data points           pdm-model-20260308       predictedPeak:       severity based on
  1min granularity            accuracy: 94.2%           88.5                 breachLikelihood
  30 days of history          algorithm: prophet        breachLikelihood:    >80% = critical
                              8500ms training           72.3%                else = warning
```

## Workers

**CollectHistory** -- Takes `metricName` and `historyDays` (default 30). Returns
`dataPoints: 43200`, `granularity: "1m"`, with timestamps spanning
`"2026-02-06"` to `"2026-03-08"`.

**TrainModel** -- Trains a prediction model on the data points. Returns
`modelId: "pdm-model-20260308"`, `accuracy: 94.2`, `algorithm: "prophet"`,
`trainingTimeMs: 8500`.

**Predict** -- Forecasts the next `forecastHours`. Returns `predictedPeak: 88.5`,
`predictedPeakTime: "2026-03-08T14:00:00Z"`, `breachLikelihood: 72.3`, and a
`confidenceInterval` of {low: 78.2, high: 95.1}.

**PdmAlert** -- Fires an alert if breach is likely. Severity is `"critical"` when
`breachLikelihood > 80`, otherwise `"warning"`. Returns `alertSent` boolean and a human-
readable message.

## Tests

20 unit tests cover history collection, model training, prediction, and alert routing.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
