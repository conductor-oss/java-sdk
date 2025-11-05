# Conductor Java SDK - Metrics & Event System Design

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Core Components](#core-components)
- [Event Lifecycle](#event-lifecycle)
- [Usage Guide](#usage-guide)
- [Extending the System](#extending-the-system)
- [Performance Considerations](#performance-considerations)
- [Examples](#examples)

## Overview

The Conductor Java SDK implements a flexible, event-driven metrics collection system that allows developers to observe and measure task and workflow execution at runtime. The design follows the **Observer pattern** with asynchronous event publishing, providing a non-blocking, extensible architecture for metrics collection and monitoring.

### Key Design Goals

1. **Non-intrusive**: Metrics collection should not impact task execution performance
2. **Extensible**: Easy to add new metrics backends (Prometheus, Datadog, CloudWatch, etc.)
3. **Type-safe**: Leverage Java's type system to prevent runtime errors
4. **Decoupled**: Clear separation between business logic and observability
5. **Asynchronous**: Event publishing and consumption should not block worker threads

## Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       Task Execution Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  TaskRunner  │  │WorkflowClient│  │  TaskClient  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│         └──────────────────┼──────────────────┘                  │
│                            │ publish events                       │
└────────────────────────────┼──────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Event Dispatch Layer                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              EventDispatcher<T>                            │  │
│  │  • Async event publishing (CompletableFuture)             │  │
│  │  • Type-safe event routing                                 │  │
│  │  • Multiple listener support                               │  │
│  └───────────────────────┬───────────────────────────────────┘  │
│                          │ dispatch to listeners                 │
└──────────────────────────┼───────────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Listener/Consumer Layer                      │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │ MetricsCollector│  │ Custom Listener│  │ Audit Logger     │  │
│  │  (Prometheus)   │  │  (Business)    │  │  (Compliance)    │  │
│  └────────────────┘  └────────────────┘  └──────────────────┘  │
│                                                                   │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────┐  │
│  │   Datadog      │  │   CloudWatch   │  │   Custom Metrics │  │
│  │   Collector    │  │   Collector    │  │   Backend        │  │
│  └────────────────┘  └────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Event Hierarchy

All events inherit from `ConductorClientEvent` which provides a timestamp for when the event occurred.

```
ConductorClientEvent (abstract)
│   • Instant time
│
├── TaskRunnerEvent (abstract)
│   │   • String taskType
│   │
│   ├── PollStarted
│   │
│   ├── PollCompleted
│   │   • Duration duration
│   │
│   ├── PollFailure
│   │   • Duration duration
│   │   • Throwable cause
│   │
│   ├── TaskExecutionStarted
│   │   • String taskId
│   │   • String workerId
│   │
│   ├── TaskExecutionCompleted
│   │   • String taskId
│   │   • String workerId
│   │   • Duration duration
│   │
│   └── TaskExecutionFailure
│       • String taskId
│       • String workerId
│       • Throwable cause
│       • Duration duration
│
├── WorkflowClientEvent (abstract)
│   │   • String name
│   │   • Integer version
│   │
│   ├── WorkflowStartedEvent
│   │   • boolean success
│   │   • Throwable throwable
│   │
│   ├── WorkflowPayloadUsedEvent
│   │   • String operation (READ/WRITE)
│   │   • String payloadType
│   │
│   └── WorkflowInputPayloadSizeEvent
│       • long size
│
└── TaskClientEvent (abstract)
    │   • String taskType
    │
    ├── TaskPayloadUsedEvent
    │   • String operation
    │   • String payloadType
    │
    └── TaskResultPayloadSizeEvent
        • long size
```

## Core Components

### 1. EventDispatcher\<T\>

**Location**: `conductor-client/src/main/java/com/netflix/conductor/client/events/dispatcher/EventDispatcher.java`

The core event routing component that manages listener registration and event publishing.

**Key Features**:
- Generic type parameter ensures type safety
- Thread-safe using `ConcurrentHashMap` and `CopyOnWriteArrayList`
- Asynchronous event publishing via `CompletableFuture.runAsync()`
- Supports both specific event type listeners and "promiscuous" listeners (listening to all events)

**API**:
```java
public class EventDispatcher<T extends ConductorClientEvent> {
    // Register a listener for a specific event type
    public <U extends T> void register(Class<U> clazz, Consumer<U> listener);

    // Unregister a listener
    public <U extends T> void unregister(Class<U> clazz, Consumer<U> listener);

    // Publish an event (async)
    public void publish(T event);
}
```

### 2. Listener Interfaces

#### TaskRunnerEventsListener

**Location**: `conductor-client/src/main/java/com/netflix/conductor/client/events/listeners/TaskRunnerEventsListener.java`

Defines callbacks for task runner lifecycle events.

```java
public interface TaskRunnerEventsListener {
    void consume(PollFailure e);
    void consume(PollCompleted e);
    void consume(PollStarted e);
    void consume(TaskExecutionStarted e);
    void consume(TaskExecutionCompleted e);
    void consume(TaskExecutionFailure e);
}
```

#### WorkflowClientListener

**Location**: `conductor-client/src/main/java/com/netflix/conductor/client/events/listeners/WorkflowClientListener.java`

Defines callbacks for workflow client events.

```java
public interface WorkflowClientListener {
    void consume(WorkflowPayloadUsedEvent event);
    void consume(WorkflowInputPayloadSizeEvent event);
    void consume(WorkflowStartedEvent event);
}
```

#### TaskClientListener

**Location**: `conductor-client/src/main/java/com/netflix/conductor/client/events/listeners/TaskClientListener.java`

Defines callbacks for task client payload events.

```java
public interface TaskClientListener {
    void consume(TaskPayloadUsedEvent e);
    void consume(TaskResultPayloadSizeEvent e);
}
```

#### MetricsCollector

**Location**: `conductor-client/src/main/java/com/netflix/conductor/client/metrics/MetricsCollector.java`

A unified interface that extends all three listener interfaces, providing a single point for comprehensive metrics collection.

```java
public interface MetricsCollector
    extends TaskRunnerEventsListener,
            WorkflowClientListener,
            TaskClientListener {
    // Inherits all consume() methods from parent interfaces
}
```

### 3. ListenerRegister

**Location**: `conductor-client/src/main/java/com/netflix/conductor/client/events/listeners/ListenerRegister.java`

Utility class for bulk registration of listeners with event dispatchers.

```java
public class ListenerRegister {
    // Register all TaskRunnerEventsListener methods
    public static void register(
        TaskRunnerEventsListener listener,
        EventDispatcher<TaskRunnerEvent> dispatcher
    );

    // Register all WorkflowClientListener methods
    public static void register(
        WorkflowClientListener listener,
        EventDispatcher<WorkflowClientEvent> dispatcher
    );

    // Register all TaskClientListener methods
    public static void register(
        TaskClientListener listener,
        EventDispatcher<TaskClientEvent> dispatcher
    );
}
```

### 4. PrometheusMetricsCollector

**Location**: `conductor-client-metrics/src/main/java/com/netflix/conductor/client/metrics/prometheus/PrometheusMetricsCollector.java`

Reference implementation of `MetricsCollector` using Micrometer Prometheus.

**Features**:
- Exposes HTTP endpoint for Prometheus scraping (default: `localhost:9991/metrics`)
- Records timers for poll duration (success/failure)
- Records timers for task execution duration (completed/failure)
- Records counters for poll started and task execution started
- All metrics tagged with task type

**Metrics Exposed**:
- `poll_failure` (timer) - Duration of failed polls
- `poll_success` (timer) - Duration of successful polls
- `poll_started` (counter) - Count of poll attempts
- `task_execution_started` (counter) - Count of task executions started
- `task_execution_completed` (timer) - Duration of completed task executions
- `task_execution_failure` (timer) - Duration of failed task executions

## Event Lifecycle

### Task Runner Event Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Worker polls for tasks                                        │
│    TaskRunner.pollTasksForWorker()                              │
│    └─→ eventDispatcher.publish(new PollStarted(taskType))      │
└─────────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Poll completes (success or failure)                          │
│    • Success: eventDispatcher.publish(                          │
│        new PollCompleted(taskType, duration))                   │
│    • Failure: eventDispatcher.publish(                          │
│        new PollFailure(taskType, duration, exception))          │
└─────────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Task execution begins                                         │
│    TaskRunner.processTask()                                     │
│    └─→ eventDispatcher.publish(                                │
│        new TaskExecutionStarted(taskType, taskId, workerId))   │
└─────────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Worker executes task                                          │
│    TaskRunner.executeTask()                                     │
│    └─→ worker.execute(task)                                     │
└─────────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Task execution completes (success or failure)                │
│    • Success: eventDispatcher.publish(                          │
│        new TaskExecutionCompleted(taskType, taskId,             │
│            workerId, duration))                                  │
│    • Failure: eventDispatcher.publish(                          │
│        new TaskExecutionFailure(taskType, taskId,               │
│            workerId, exception, duration))                       │
└─────────────────────────────────────────────────────────────────┘
```

**Source Code References**:
- Poll started: `TaskRunner.java:227`
- Poll completed: `TaskRunner.java:266`
- Poll failure: `TaskRunner.java:286`
- Task execution started: `TaskRunner.java:316`
- Task execution completed: `TaskRunner.java:371`
- Task execution failure: `TaskRunner.java:381`

### Workflow Client Event Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Check payload size                                            │
│    WorkflowClient.checkAndUploadToExternalStorage()            │
│    └─→ eventDispatcher.publish(                                │
│        new WorkflowInputPayloadSizeEvent(name, version, size)) │
└─────────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Upload to external storage (if needed)                       │
│    └─→ eventDispatcher.publish(                                │
│        new WorkflowPayloadUsedEvent(name, version,             │
│            "WRITE", "WORKFLOW_INPUT"))                          │
└─────────────────────────────────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Start workflow                                                │
│    WorkflowClient.startWorkflow()                               │
│    • Success: eventDispatcher.publish(                          │
│        new WorkflowStartedEvent(name, version))                 │
│    • Failure: eventDispatcher.publish(                          │
│        new WorkflowStartedEvent(name, version, false, error))  │
└─────────────────────────────────────────────────────────────────┘
```

**Source Code References**:
- Payload size: `WorkflowClient.java:135-136`
- Payload used: `WorkflowClient.java:145-148`, `WorkflowClient.java:570-573`
- Workflow started: `WorkflowClient.java:126`, `WorkflowClient.java:162-163`

## Usage Guide

### Basic Setup with Prometheus Metrics

```java
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector;

// 1. Create TaskClient
TaskClient taskClient = new TaskClient("http://conductor-server:8080");

// 2. Create and start PrometheusMetricsCollector
PrometheusMetricsCollector metricsCollector = new PrometheusMetricsCollector();
metricsCollector.startServer(); // Starts HTTP server on port 9991

// 3. Configure TaskRunner with metrics
TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
    .withThreadCount(10)
    .withMetricsCollector(metricsCollector)
    .build();

// 4. Start polling
configurer.init();
```

### Custom Metrics Endpoint

```java
// Start Prometheus server on custom port and endpoint
PrometheusMetricsCollector metricsCollector = new PrometheusMetricsCollector();
metricsCollector.startServer(8080, "/custom-metrics");
```

### Registering Custom Event Listeners

#### Approach 1: Using Builder's Listener API

```java
TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
    // Listen to specific event types
    .withListener(TaskExecutionCompleted.class, event -> {
        System.out.println("Task " + event.getTaskId() +
                          " completed in " + event.getDuration().toMillis() + "ms");
    })
    .withListener(TaskExecutionFailure.class, event -> {
        System.err.println("Task " + event.getTaskId() +
                          " failed: " + event.getCause().getMessage());
    })
    // Listen to ALL task runner events
    .withListener(TaskRunnerEvent.class, event -> {
        System.out.println("Event: " + event.getClass().getSimpleName() +
                          " for task type: " + event.getTaskType());
    })
    .build();
```

#### Approach 2: Implementing Custom MetricsCollector

```java
public class CustomMetricsCollector implements MetricsCollector {

    private final MetricRegistry registry = new MetricRegistry();

    @Override
    public void consume(PollCompleted e) {
        registry.timer("poll.success." + e.getTaskType())
               .update(e.getDuration().toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        registry.timer("task.execution." + e.getTaskType())
               .update(e.getDuration().toMillis(), TimeUnit.MILLISECONDS);

        // Custom business logic
        if (e.getDuration().toSeconds() > 300) {
            alertService.sendAlert("Slow task detected: " + e.getTaskId());
        }
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        registry.counter("task.failures." + e.getTaskType()).inc();

        // Send to error tracking service
        errorTracker.captureException(e.getCause(),
            Map.of(
                "taskType", e.getTaskType(),
                "taskId", e.getTaskId(),
                "workerId", e.getWorkerId()
            )
        );
    }

    // Implement remaining interface methods...
    @Override
    public void consume(PollFailure e) { /* ... */ }

    @Override
    public void consume(PollStarted e) { /* ... */ }

    @Override
    public void consume(TaskExecutionStarted e) { /* ... */ }

    @Override
    public void consume(TaskPayloadUsedEvent e) { /* ... */ }

    @Override
    public void consume(TaskResultPayloadSizeEvent e) { /* ... */ }

    @Override
    public void consume(WorkflowPayloadUsedEvent event) { /* ... */ }

    @Override
    public void consume(WorkflowInputPayloadSizeEvent event) { /* ... */ }

    @Override
    public void consume(WorkflowStartedEvent event) { /* ... */ }
}

// Usage
TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
    .withMetricsCollector(new CustomMetricsCollector())
    .build();
```

#### Approach 3: Implementing Specific Listener Interfaces

If you only need task runner events:

```java
public class TaskMonitor implements TaskRunnerEventsListener {

    @Override
    public void consume(PollStarted e) {
        log.debug("Starting poll for: {}", e.getTaskType());
    }

    @Override
    public void consume(PollCompleted e) {
        log.debug("Poll completed for {} in {}ms",
                 e.getTaskType(), e.getDuration().toMillis());
    }

    @Override
    public void consume(PollFailure e) {
        log.error("Poll failed for {}: {}",
                 e.getTaskType(), e.getCause().getMessage());
    }

    @Override
    public void consume(TaskExecutionStarted e) {
        log.info("Task {} started on worker {}",
                e.getTaskId(), e.getWorkerId());
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        log.info("Task {} completed in {}ms",
                e.getTaskId(), e.getDuration().toMillis());
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        log.error("Task {} failed: {}",
                 e.getTaskId(), e.getCause().getMessage());
    }
}

// Register manually using EventDispatcher
EventDispatcher<TaskRunnerEvent> dispatcher = new EventDispatcher<>();
ListenerRegister.register(new TaskMonitor(), dispatcher);
```

### Workflow and Task Client Event Listeners

```java
WorkflowClient workflowClient = new WorkflowClient("http://conductor-server:8080");
TaskClient taskClient = new TaskClient("http://conductor-server:8080");

// Register workflow listener
workflowClient.registerListener(new WorkflowClientListener() {
    @Override
    public void consume(WorkflowStartedEvent event) {
        if (event.isSuccess()) {
            log.info("Workflow {} v{} started successfully",
                    event.getName(), event.getVersion());
        } else {
            log.error("Workflow {} v{} failed to start",
                     event.getName(), event.getVersion(), event.getThrowable());
        }
    }

    @Override
    public void consume(WorkflowInputPayloadSizeEvent event) {
        if (event.getSize() > 1024 * 1024) { // 1MB
            log.warn("Large workflow input: {} bytes for {} v{}",
                    event.getSize(), event.getName(), event.getVersion());
        }
    }

    @Override
    public void consume(WorkflowPayloadUsedEvent event) {
        log.debug("External storage {} for {} - {}",
                 event.getOperation(), event.getName(), event.getPayloadType());
    }
});

// Register task client listener
taskClient.registerListener(new TaskClientListener() {
    @Override
    public void consume(TaskPayloadUsedEvent e) {
        log.debug("External storage used for task payload");
    }

    @Override
    public void consume(TaskResultPayloadSizeEvent e) {
        log.debug("Task result payload size: {} bytes", e.getSize());
    }
});
```

## Extending the System

### Adding New Event Types

#### 1. Create New Event Class

```java
package com.netflix.conductor.client.events.taskrunner;

import java.time.Duration;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class TaskRetried extends TaskRunnerEvent {
    private final String taskId;
    private final String workerId;
    private final int retryCount;
    private final Duration backoffDuration;

    public TaskRetried(String taskType, String taskId, String workerId,
                       int retryCount, long backoffMillis) {
        super(taskType);
        this.taskId = taskId;
        this.workerId = workerId;
        this.retryCount = retryCount;
        this.backoffDuration = Duration.ofMillis(backoffMillis);
    }
}
```

#### 2. Publish Event from Source

```java
// In TaskRunner.java or wherever the event occurs
eventDispatcher.publish(new TaskRetried(
    task.getTaskDefName(),
    task.getTaskId(),
    worker.getIdentity(),
    task.getRetryCount(),
    backoffTime
));
```

#### 3. Add to Listener Interface (Optional)

If you want to include it in the standard listener interface:

```java
public interface TaskRunnerEventsListener {
    // ... existing methods ...

    void consume(TaskRetried e);
}
```

#### 4. Update ListenerRegister (If Updated Interface)

```java
public static void register(TaskRunnerEventsListener listener,
                           EventDispatcher<TaskRunnerEvent> dispatcher) {
    // ... existing registrations ...
    dispatcher.register(TaskRetried.class, listener::consume);
}
```

### Creating Custom Metrics Backends

#### Example: Datadog Metrics Collector

```java
package com.example.conductor.metrics;

import com.netflix.conductor.client.metrics.MetricsCollector;
import com.netflix.conductor.client.events.taskrunner.*;
import com.netflix.conductor.client.events.workflow.*;
import com.netflix.conductor.client.events.task.*;
import com.datadog.api.client.ApiClient;
import com.datadog.api.client.v2.api.MetricsApi;
import com.datadog.api.client.v2.model.*;

public class DatadogMetricsCollector implements MetricsCollector {

    private final MetricsApi metricsApi;
    private final String namespace;

    public DatadogMetricsCollector(String apiKey, String namespace) {
        ApiClient client = ApiClient.getDefaultApiClient();
        client.setApiKey(apiKey);
        this.metricsApi = new MetricsApi(client);
        this.namespace = namespace;
    }

    @Override
    public void consume(PollCompleted e) {
        sendMetric(
            namespace + ".poll.duration",
            e.getDuration().toMillis(),
            Map.of("task_type", e.getTaskType(), "status", "success")
        );
    }

    @Override
    public void consume(PollFailure e) {
        sendMetric(
            namespace + ".poll.duration",
            e.getDuration().toMillis(),
            Map.of("task_type", e.getTaskType(), "status", "failure")
        );

        sendMetric(
            namespace + ".poll.errors",
            1,
            Map.of(
                "task_type", e.getTaskType(),
                "error_type", e.getCause().getClass().getSimpleName()
            )
        );
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        sendMetric(
            namespace + ".task.execution.duration",
            e.getDuration().toMillis(),
            Map.of(
                "task_type", e.getTaskType(),
                "worker_id", e.getWorkerId(),
                "status", "completed"
            )
        );
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        sendMetric(
            namespace + ".task.execution.duration",
            e.getDuration().toMillis(),
            Map.of(
                "task_type", e.getTaskType(),
                "worker_id", e.getWorkerId(),
                "status", "failed"
            )
        );

        sendMetric(
            namespace + ".task.execution.errors",
            1,
            Map.of(
                "task_type", e.getTaskType(),
                "error_type", e.getCause().getClass().getSimpleName()
            )
        );
    }

    // Implement remaining interface methods...

    private void sendMetric(String metricName, double value, Map<String, String> tags) {
        try {
            MetricPayload payload = new MetricPayload()
                .series(List.of(
                    new MetricSeries()
                        .metric(metricName)
                        .points(List.of(
                            new MetricPoint()
                                .timestamp(System.currentTimeMillis() / 1000)
                                .value(value)
                        ))
                        .tags(tags.entrySet().stream()
                            .map(e -> e.getKey() + ":" + e.getValue())
                            .collect(Collectors.toList()))
                ));

            metricsApi.submitMetrics(payload);
        } catch (Exception ex) {
            // Log error but don't fail
            System.err.println("Failed to send metric to Datadog: " + ex.getMessage());
        }
    }
}
```

#### Example: CloudWatch Metrics Collector

```java
package com.example.conductor.metrics;

import com.netflix.conductor.client.metrics.MetricsCollector;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

public class CloudWatchMetricsCollector implements MetricsCollector {

    private final CloudWatchClient cloudWatch;
    private final String namespace;

    public CloudWatchMetricsCollector(CloudWatchClient cloudWatch, String namespace) {
        this.cloudWatch = cloudWatch;
        this.namespace = namespace;
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        putMetric(
            "TaskExecutionDuration",
            e.getDuration().toMillis(),
            StandardUnit.MILLISECONDS,
            Dimension.builder()
                .name("TaskType")
                .value(e.getTaskType())
                .build(),
            Dimension.builder()
                .name("Status")
                .value("Completed")
                .build()
        );
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        putMetric(
            "TaskExecutionDuration",
            e.getDuration().toMillis(),
            StandardUnit.MILLISECONDS,
            Dimension.builder()
                .name("TaskType")
                .value(e.getTaskType())
                .build(),
            Dimension.builder()
                .name("Status")
                .value("Failed")
                .build()
        );

        putMetric(
            "TaskExecutionErrors",
            1.0,
            StandardUnit.COUNT,
            Dimension.builder()
                .name("TaskType")
                .value(e.getTaskType())
                .build(),
            Dimension.builder()
                .name("ErrorType")
                .value(e.getCause().getClass().getSimpleName())
                .build()
        );
    }

    // Implement remaining interface methods...

    private void putMetric(String metricName, double value,
                          StandardUnit unit, Dimension... dimensions) {
        try {
            MetricDatum datum = MetricDatum.builder()
                .metricName(metricName)
                .value(value)
                .unit(unit)
                .timestamp(Instant.now())
                .dimensions(dimensions)
                .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                .namespace(namespace)
                .metricData(datum)
                .build();

            cloudWatch.putMetricData(request);
        } catch (Exception ex) {
            System.err.println("Failed to send metric to CloudWatch: " + ex.getMessage());
        }
    }
}
```

### Running Multiple Collectors Simultaneously

```java
// Create multiple collectors
PrometheusMetricsCollector prometheus = new PrometheusMetricsCollector();
prometheus.startServer(9991, "/metrics");

DatadogMetricsCollector datadog = new DatadogMetricsCollector(
    System.getenv("DATADOG_API_KEY"),
    "conductor"
);

CloudWatchMetricsCollector cloudwatch = new CloudWatchMetricsCollector(
    CloudWatchClient.create(),
    "ConductorMetrics"
);

// Register all collectors
TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
    .withMetricsCollector(prometheus)
    .withMetricsCollector(datadog)
    .withMetricsCollector(cloudwatch)
    .build();

configurer.init();
```

### Building Custom Observability Solutions

#### Example: Audit Logger with Filtering

```java
public class AuditLogger implements TaskRunnerEventsListener {

    private final Set<String> auditedTaskTypes;
    private final AuditRepository repository;

    public AuditLogger(Set<String> auditedTaskTypes, AuditRepository repository) {
        this.auditedTaskTypes = auditedTaskTypes;
        this.repository = repository;
    }

    @Override
    public void consume(TaskExecutionStarted e) {
        if (shouldAudit(e.getTaskType())) {
            repository.save(AuditEvent.builder()
                .eventType("TASK_STARTED")
                .timestamp(Instant.now())
                .taskType(e.getTaskType())
                .taskId(e.getTaskId())
                .workerId(e.getWorkerId())
                .build());
        }
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        if (shouldAudit(e.getTaskType())) {
            repository.save(AuditEvent.builder()
                .eventType("TASK_COMPLETED")
                .timestamp(Instant.now())
                .taskType(e.getTaskType())
                .taskId(e.getTaskId())
                .workerId(e.getWorkerId())
                .duration(e.getDuration())
                .build());
        }
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        if (shouldAudit(e.getTaskType())) {
            repository.save(AuditEvent.builder()
                .eventType("TASK_FAILED")
                .timestamp(Instant.now())
                .taskType(e.getTaskType())
                .taskId(e.getTaskId())
                .workerId(e.getWorkerId())
                .duration(e.getDuration())
                .error(e.getCause().getMessage())
                .stackTrace(getStackTrace(e.getCause()))
                .build());
        }
    }

    private boolean shouldAudit(String taskType) {
        return auditedTaskTypes.isEmpty() || auditedTaskTypes.contains(taskType);
    }

    // Implement remaining interface methods...
}
```

#### Example: SLA Monitor with Alerting

```java
public class SLAMonitor implements TaskRunnerEventsListener {

    private final Map<String, Duration> slaThresholds;
    private final AlertService alertService;

    public SLAMonitor(Map<String, Duration> slaThresholds, AlertService alertService) {
        this.slaThresholds = slaThresholds;
        this.alertService = alertService;
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        Duration threshold = slaThresholds.get(e.getTaskType());
        if (threshold != null && e.getDuration().compareTo(threshold) > 0) {
            alertService.sendAlert(Alert.builder()
                .severity(AlertSeverity.WARNING)
                .title("SLA Violation")
                .message(String.format(
                    "Task %s exceeded SLA threshold. Expected: %dms, Actual: %dms",
                    e.getTaskId(),
                    threshold.toMillis(),
                    e.getDuration().toMillis()
                ))
                .tags(Map.of(
                    "task_type", e.getTaskType(),
                    "task_id", e.getTaskId(),
                    "worker_id", e.getWorkerId()
                ))
                .build());
        }
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        alertService.sendAlert(Alert.builder()
            .severity(AlertSeverity.ERROR)
            .title("Task Execution Failure")
            .message(String.format(
                "Task %s failed: %s",
                e.getTaskId(),
                e.getCause().getMessage()
            ))
            .tags(Map.of(
                "task_type", e.getTaskType(),
                "task_id", e.getTaskId(),
                "worker_id", e.getWorkerId(),
                "error_type", e.getCause().getClass().getSimpleName()
            ))
            .build());
    }

    // Implement remaining interface methods...
}
```

#### Example: Real-time Dashboard Publisher

```java
public class DashboardPublisher implements MetricsCollector {

    private final WebSocketSession dashboardSession;
    private final ObjectMapper objectMapper;

    public DashboardPublisher(WebSocketSession dashboardSession) {
        this.dashboardSession = dashboardSession;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        sendToDashboard(DashboardEvent.builder()
            .type("task_completed")
            .timestamp(Instant.now())
            .data(Map.of(
                "taskType", e.getTaskType(),
                "taskId", e.getTaskId(),
                "workerId", e.getWorkerId(),
                "duration", e.getDuration().toMillis()
            ))
            .build());
    }

    @Override
    public void consume(TaskExecutionFailure e) {
        sendToDashboard(DashboardEvent.builder()
            .type("task_failed")
            .timestamp(Instant.now())
            .data(Map.of(
                "taskType", e.getTaskType(),
                "taskId", e.getTaskId(),
                "workerId", e.getWorkerId(),
                "duration", e.getDuration().toMillis(),
                "error", e.getCause().getMessage()
            ))
            .build());
    }

    private void sendToDashboard(DashboardEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            dashboardSession.sendMessage(new TextMessage(json));
        } catch (Exception ex) {
            // Log but don't fail
        }
    }

    // Implement remaining interface methods...
}
```

### Advanced Use Cases

#### Cost Tracking and Optimization

```java
public class CostTracker implements TaskRunnerEventsListener {

    private final Map<String, BigDecimal> taskTypeCostPerSecond;
    private final CostRepository costRepository;

    public CostTracker(Map<String, BigDecimal> taskTypeCostPerSecond,
                      CostRepository costRepository) {
        this.taskTypeCostPerSecond = taskTypeCostPerSecond;
        this.costRepository = costRepository;
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        BigDecimal costPerSecond = taskTypeCostPerSecond.get(e.getTaskType());
        if (costPerSecond != null) {
            BigDecimal executionSeconds = BigDecimal.valueOf(
                e.getDuration().toMillis() / 1000.0
            );
            BigDecimal cost = costPerSecond.multiply(executionSeconds);

            costRepository.recordCost(CostRecord.builder()
                .timestamp(Instant.now())
                .taskType(e.getTaskType())
                .taskId(e.getTaskId())
                .workerId(e.getWorkerId())
                .durationSeconds(executionSeconds)
                .cost(cost)
                .build());
        }
    }

    // Implement remaining interface methods...
}
```

#### Dynamic Worker Scaling

```java
public class WorkerScaler implements TaskRunnerEventsListener {

    private final Map<String, Integer> pollCounters = new ConcurrentHashMap<>();
    private final Map<String, Integer> executionCounters = new ConcurrentHashMap<>();
    private final WorkerPoolManager poolManager;

    public WorkerScaler(WorkerPoolManager poolManager) {
        this.poolManager = poolManager;
    }

    @Override
    public void consume(PollStarted e) {
        pollCounters.merge(e.getTaskType(), 1, Integer::sum);
        checkScaling(e.getTaskType());
    }

    @Override
    public void consume(TaskExecutionStarted e) {
        executionCounters.merge(e.getTaskType(), 1, Integer::sum);
    }

    @Override
    public void consume(TaskExecutionCompleted e) {
        executionCounters.merge(e.getTaskType(), -1, Integer::sum);
        checkScaling(e.getTaskType());
    }

    private void checkScaling(String taskType) {
        int activeExecutions = executionCounters.getOrDefault(taskType, 0);
        int currentWorkers = poolManager.getWorkerCount(taskType);

        // Scale up if all workers are busy
        if (activeExecutions >= currentWorkers * 0.9) {
            poolManager.scaleUp(taskType);
        }

        // Scale down if workers are idle
        if (activeExecutions <= currentWorkers * 0.3) {
            poolManager.scaleDown(taskType);
        }
    }

    // Implement remaining interface methods...
}
```

## Performance Considerations

### Async Event Publishing

All events are published asynchronously using `CompletableFuture.runAsync()`, which ensures:
- **Non-blocking**: Task execution is never blocked by event processing
- **Independent failure**: If a listener throws an exception, it doesn't affect task execution
- **Scalability**: Event processing can scale independently

### Thread Safety

- **CopyOnWriteArrayList**: Used for listener storage, optimized for read-heavy workloads
- **ConcurrentHashMap**: Used for event type to listener mappings
- **Immutable Events**: All event objects are immutable (final fields), preventing race conditions

### Memory Considerations

- Events are short-lived objects that are garbage collected quickly
- Listeners should avoid blocking operations or long-running computations
- Consider buffering/batching when sending metrics to external systems

### Best Practices

1. **Keep Listeners Lightweight**: Avoid heavy computations in event handlers
2. **Async External Calls**: Use async clients when sending data to external services
3. **Error Handling**: Always catch exceptions in listeners to prevent failures
4. **Batching**: Batch metrics when possible to reduce network overhead
5. **Sampling**: For high-throughput systems, consider sampling events

### Performance Impact Measurement

```java
// Example: Measuring overhead of metrics collection
public class MetricsOverheadMonitor implements TaskRunnerEventsListener {

    private final AtomicLong totalEvents = new AtomicLong();
    private final AtomicLong totalProcessingTime = new AtomicLong();

    @Override
    public void consume(TaskExecutionCompleted e) {
        long start = System.nanoTime();

        // Your metrics logic here

        long duration = System.nanoTime() - start;
        totalEvents.incrementAndGet();
        totalProcessingTime.addAndGet(duration);
    }

    public double getAverageOverheadMicros() {
        long events = totalEvents.get();
        if (events == 0) return 0;
        return totalProcessingTime.get() / (double) events / 1000.0;
    }

    // Implement remaining interface methods...
}
```

## Examples

### Complete Example: Multi-Backend Monitoring Setup

```java
package com.example.conductor;

import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.client.metrics.prometheus.PrometheusMetricsCollector;
import java.util.List;

public class ConductorMonitoringSetup {

    public static void main(String[] args) throws Exception {
        // 1. Create clients
        TaskClient taskClient = new TaskClient("http://conductor-server:8080");

        // 2. Create workers
        List<Worker> workers = List.of(
            new MyTaskWorker(),
            new AnotherTaskWorker()
        );

        // 3. Setup Prometheus metrics
        PrometheusMetricsCollector prometheus = new PrometheusMetricsCollector();
        prometheus.startServer(9991, "/metrics");

        // 4. Setup custom monitoring
        SLAMonitor slaMonitor = new SLAMonitor(
            Map.of(
                "my_task", Duration.ofSeconds(30),
                "another_task", Duration.ofSeconds(60)
            ),
            new PagerDutyAlertService()
        );

        AuditLogger auditLogger = new AuditLogger(
            Set.of("sensitive_task", "compliance_task"),
            new DatabaseAuditRepository()
        );

        // 5. Setup cost tracking
        CostTracker costTracker = new CostTracker(
            Map.of(
                "expensive_task", new BigDecimal("0.01"), // $0.01 per second
                "cheap_task", new BigDecimal("0.001")
            ),
            new CostDatabaseRepository()
        );

        // 6. Configure task runner with all monitoring
        TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)
            .withThreadCount(20)
            .withMetricsCollector(prometheus)
            .withListener(TaskExecutionCompleted.class, slaMonitor::consume)
            .withListener(TaskExecutionFailure.class, slaMonitor::consume)
            .withListener(TaskExecutionStarted.class, auditLogger::consume)
            .withListener(TaskExecutionCompleted.class, auditLogger::consume)
            .withListener(TaskExecutionFailure.class, auditLogger::consume)
            .withListener(TaskExecutionCompleted.class, costTracker::consume)
            .build();

        // 7. Start polling
        configurer.init();

        // 8. Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gracefully...");
            configurer.shutdown();
        }));

        System.out.println("Conductor workers started with comprehensive monitoring");
        System.out.println("Prometheus metrics: http://localhost:9991/metrics");
    }
}
```

### Example: Testing with Event Assertions

```java
package com.example.conductor;

import com.netflix.conductor.client.events.dispatcher.EventDispatcher;
import com.netflix.conductor.client.events.taskrunner.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class EventSystemTest {

    @Test
    public void testEventPublishingAndConsumption() throws Exception {
        // Setup
        EventDispatcher<TaskRunnerEvent> dispatcher = new EventDispatcher<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);

        // Register listener
        dispatcher.register(TaskExecutionCompleted.class, event -> {
            eventCount.incrementAndGet();
            assertEquals("test_task", event.getTaskType());
            assertEquals("task-123", event.getTaskId());
            latch.countDown();
        });

        // Publish event
        dispatcher.publish(new TaskExecutionCompleted(
            "test_task",
            "task-123",
            "worker-1",
            1000L
        ));

        // Wait for async processing
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, eventCount.get());
    }

    @Test
    public void testMultipleListeners() throws Exception {
        EventDispatcher<TaskRunnerEvent> dispatcher = new EventDispatcher<>();
        CountDownLatch latch = new CountDownLatch(2);

        dispatcher.register(TaskExecutionCompleted.class, event -> {
            System.out.println("Listener 1: " + event.getTaskId());
            latch.countDown();
        });

        dispatcher.register(TaskExecutionCompleted.class, event -> {
            System.out.println("Listener 2: " + event.getTaskId());
            latch.countDown();
        });

        dispatcher.publish(new TaskExecutionCompleted(
            "test_task", "task-123", "worker-1", 1000L
        ));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
```

## Troubleshooting

### Events Not Being Published

**Problem**: Metrics collector not receiving events

**Solution**:
1. Verify listener registration:
   ```java
   // Ensure you're using the builder method
   builder.withMetricsCollector(metricsCollector)
   ```

2. Check if TaskRunnerConfigurer.init() was called:
   ```java
   configurer.init(); // Required to start event publishing
   ```

3. Verify WorkflowClient/TaskClient listeners are registered:
   ```java
   workflowClient.registerListener(metricsCollector);
   taskClient.registerListener(metricsCollector);
   ```

### Prometheus Metrics Not Showing Up

**Problem**: Prometheus endpoint returns no metrics

**Solution**:
1. Verify server is started:
   ```java
   metricsCollector.startServer(); // Don't forget this!
   ```

2. Check port availability:
   ```bash
   lsof -i :9991
   ```

3. Verify tasks are being executed (metrics only appear after events)

### Performance Issues

**Problem**: High CPU or memory usage

**Solution**:
1. Avoid blocking operations in listeners
2. Use sampling for high-throughput systems:
   ```java
   private final Random random = new Random();

   @Override
   public void consume(TaskExecutionCompleted e) {
       if (random.nextDouble() < 0.1) { // Sample 10%
           // Process event
       }
   }
   ```

3. Batch external API calls:
   ```java
   private final List<MetricEvent> buffer = new ArrayList<>();

   @Override
   public void consume(TaskExecutionCompleted e) {
       synchronized (buffer) {
           buffer.add(e);
           if (buffer.size() >= 100) {
               flushBuffer();
           }
       }
   }
   ```

### Thread Safety Issues

**Problem**: Concurrent modification exceptions or race conditions

**Solution**:
- Use thread-safe collections in listeners
- Avoid shared mutable state
- Use atomic operations or synchronization when necessary

## Summary

The Conductor Java SDK's metrics system provides:

✅ **Flexible, event-driven architecture**
✅ **Non-blocking, async event publishing**
✅ **Type-safe event handling**
✅ **Easy integration with multiple metrics backends**
✅ **Rich event data for comprehensive observability**
✅ **Extensible design for custom use cases**

The system is production-ready and can scale from simple Prometheus metrics to complex multi-backend monitoring solutions with custom business logic, alerting, and compliance tracking.

For more information, see:
- [Conductor OSS Documentation](https://www.conductor-oss.org/)
- [Java SDK README](README.md)
- [Conductor Client README](conductor-client/README.md)
