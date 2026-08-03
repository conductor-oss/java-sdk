# Workers

Workers execute `SIMPLE` tasks that Conductor schedules. Start with the maintained [Hello World](../examples/basics/hello-world/README.md): it registers `greet`, starts a Java worker, executes a workflow, and waits for completion.

## Worker contract

For every `SIMPLE` task:

1. The workflow task `name`, task definition name, and worker task name must match exactly.
2. At least one live worker must poll that task name.
3. The worker must be idempotent because Conductor may redeliver after a timeout or retry.

## Implement `Worker`

This is the most direct pattern and is used by Hello World:

```java
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public final class GreetWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "greet";
    }

    @Override
    public TaskResult execute(Task task) {
        String name = (String) task.getInputData().getOrDefault("name", "World");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("greeting", "Hello, " + name + "!");
        return result;
    }
}
```

Run workers with `TaskRunnerConfigurer`:

```java
import java.util.List;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;

TaskRunnerConfigurer workers = new TaskRunnerConfigurer.Builder(taskClient, List.of(new GreetWorker()))
        .withThreadCount(1)
        .build();
workers.init();
```

Call `workers.shutdown()` during application shutdown.

## Annotation-based workers

For method binding, use `@WorkerTask`, `@InputParam`, and `@OutputParam`:

```java
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;

public final class GreetingTasks {
    @WorkerTask("greet")
    public @OutputParam("greeting") String greet(@InputParam("name") String name) {
        return "Hello, " + name + "!";
    }
}
```

`WorkflowExecutor` can discover public annotated methods from packages, classes, or existing instances. Existing instances preserve constructor injection:

```java
import java.util.List;

import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

WorkflowExecutor executor = new WorkflowExecutor("http://localhost:8080/api");
executor.initWorkersFromInstances(List.of(new GreetingTasks()));
```

See the [current WorkflowExecutor source](https://github.com/conductor-oss/java-sdk/blob/main/conductor-client/src/main/java/com/netflix/conductor/sdk/workflow/executor/WorkflowExecutor.java) for all worker-loading options.

## Production guidance

- Set task-definition retry and timeout policy deliberately; a worker timeout is not a business timeout.
- Keep workers stateless and make side effects idempotent with a request key or durable application record.
- Use task domains and independent worker deployments when different queues need isolation.
- Return `FAILED_WITH_TERMINAL_ERROR` only for non-retryable failures; let transient failures use the configured retry policy.

For a local-server workflow test, continue with the [test harness](workflow-testing.md).
