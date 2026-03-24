# Lead Nurturing

Orchestrates lead nurturing through a multi-stage Conductor workflow.

**Input:** `leadId`, `leadStage`, `interests` | **Timeout:** 60s

## Pipeline

```
nur_segment
    │
nur_personalize
    │
nur_send
    │
nur_track
```

## Workers

**PersonalizeWorker** (`nur_personalize`)

```java
String topic = interests.isEmpty() ? "our solutions" : interests.get(0);
```

Reads `interests`, `segment`. Outputs `content`.

**SegmentWorker** (`nur_segment`)

```java
String segment = switch (stage) {
```

Reads `leadId`, `stage`. Outputs `segment`, `nurturePath`.

**SendWorker** (`nur_send`)

```java
String deliveryId = "DLV-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
```

Outputs `sent`, `deliveryId`, `channel`, `sentAt`.

**TrackWorker** (`nur_track`)

Reads `deliveryId`. Outputs `engagement`.

## Tests

**12 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
