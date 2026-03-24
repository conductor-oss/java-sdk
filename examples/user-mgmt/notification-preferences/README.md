# Notification Preferences

Orchestrates notification preferences through a multi-stage Conductor workflow.

**Input:** `userId`, `preferences` | **Timeout:** 60s

## Pipeline

```
np_load
    │
np_update
    │
np_sync_channels
    │
np_confirm
```

## Workers

**ConfirmPrefsWorker** (`np_confirm`): Sends confirmation for updated notification preferences.

Outputs `confirmed`.

**LoadPrefsWorker** (`np_load`): Loads current notification preferences.

Reads `userId`. Outputs `current`.

**SyncChannelsWorker** (`np_sync_channels`): Syncs active notification channels.

```java
for (Map.Entry<String, Object> entry : prefs.entrySet()) {
```

Reads `updated`. Outputs `syncedChannels`, `syncedAt`.

**UpdatePrefsWorker** (`np_update`): Merges and updates notification preferences.

```java
if (current != null) merged.putAll(current);
```

Reads `current`, `newPrefs`. Outputs `updated`.

## Tests

**14 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
