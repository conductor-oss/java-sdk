# Feature Engineering

A data science team needs to transform raw user-behavior logs into model-ready features. Each record needs numeric feature extraction, normalization to a 0-1 range, and selection of the top-k most informative features before it can be fed to a training job. Doing this ad hoc in a notebook means the feature pipeline is never reproducible.

## Pipeline

```
[fe_extract_features]
     |
     v
[fe_transform_features]
     |
     v
[fe_normalize_features]
     |
     v
[fe_validate_features]
```

**Workflow inputs:** `rawData`, `featureConfig`

## Workers

**ExtractFeaturesWorker** (task: `fe_extract_features`)

Extracts raw features from input records.

- Reads `rawData`. Writes `features`, `featureCount`

**NormalizeFeaturesWorker** (task: `fe_normalize_features`)

Min-max normalizes features to [0,1] range.

- Rounds with `math.round()`
- Reads `transformedFeatures`. Writes `normalized`, `normalizedCount`, `stats`

**TransformFeaturesWorker** (task: `fe_transform_features`)

Transforms features by adding derived features (log, polynomial, ratio).

- Rounds with `math.round()`
- Reads `features`. Writes `transformed`, `transformedCount`

**ValidateFeaturesWorker** (task: `fe_validate_features`)

Validates normalized features are in [0,1] range with no nulls.

- Reads `normalizedFeatures`. Writes `passed`, `allInRange`, `hasNulls`, `featureVector`

---

**33 tests** | Workflow: `feature_engineering` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
