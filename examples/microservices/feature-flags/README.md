# Feature Flag Routing with Legacy/New Path SWITCH

Your app has a new checkout UI, but you only want to show it to flagged users. This workflow
checks the flag status for a user (from LaunchDarkly), routes through a SWITCH to either the
new feature path (v2 UI), legacy path (v1 UI), or default path, and logs the flag usage.

## Workflow

```
userId, featureName
        |
        v
+-------------------+
| ff_check_flag     |   flagStatus: enabled/disabled, source: "launchdarkly"
+-------------------+
        |
        v
   SWITCH on flagStatus
   +--"enabled"----------+--"disabled"---------+--default-----------+
   | ff_new_feature      | ff_legacy_path      | ff_default_path    |
   | path: "new"         | path: "legacy"      | path: "default"    |
   | uiVersion: "v2"     | uiVersion: "v1"     | uiVersion: "v1"    |
   +---------------------+---------------------+--------------------+
        |
        v
   +------------------+
   | ff_log_usage     |   logged: true
   +------------------+
```

## Workers

**CheckFlagWorker** -- Checks flag `featureName` for `userId`. Returns
`flagStatus: "enabled"/"disabled"`, `rolloutPercent`, `source: "launchdarkly"`.

**NewFeatureWorker** -- Renders the new feature path. Returns `path: "new"`,
`uiVersion: "v2"`, `rendered: true`.

**LegacyPathWorker** -- Renders the legacy path. Returns `path: "legacy"`,
`uiVersion: "v1"`.

**DefaultPathWorker** -- Renders the default path. Returns `path: "default"`,
`uiVersion: "v1"`.

**LogUsageWorker** -- Logs the flag usage decision. Returns `logged: true`.

## Tests

12 unit tests cover flag checking, all three routing paths, and usage logging.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
