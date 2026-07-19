# Workflow test harness

`WorkflowTestRunner` downloads and starts a real Conductor server, then registers annotated workers and workflow metadata against it. It is an integration-test harness, not a mock-only unit-test framework.

Use it to verify input/output wiring, retries, dynamic branches, and worker behavior without managing a separate server process.

## JUnit setup

The maintained SDK test currently exercises Conductor `3.21.23`.

```java
import com.netflix.conductor.sdk.testing.WorkflowTestRunner;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class QuoteWorkflowTest {
    private static WorkflowTestRunner testRunner;
    private static WorkflowExecutor executor;

    @BeforeAll
    static void startServer() throws Exception {
        testRunner = new WorkflowTestRunner(8096, "3.21.23");
        testRunner.init("com.example.workflowtests");
        executor = testRunner.getWorkflowExecutor();
        executor.loadTaskDefs("/tasks.json");
        executor.loadWorkflowDefs("/quote_workflow.json");
    }

    @AfterAll
    static void stopServer() {
        testRunner.shutdown();
    }
}
```

The package passed to `init` must contain public `@WorkerTask` methods. The JSON files are classpath resources.

## Execute and assert

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import com.netflix.conductor.common.run.Workflow;

Workflow workflow = executor.executeWorkflow(
        "quote_workflow", 1, Map.of("name", "personA", "amount", 1_000_000))
        .get();

assertNotNull(workflow);
assertEquals(Workflow.WorkflowStatus.COMPLETED, workflow.getStatus());
assertNotNull(workflow.getOutput());
```

The full maintained example is [WorkflowTestFrameworkTests](https://github.com/conductor-oss/java-sdk/blob/main/conductor-client/src/test/java/com/netflix/conductor/sdk/workflow/testing/WorkflowTestFrameworkTests.java).

## What to cover

- Missing or invalid inputs produce the expected workflow status.
- Retryable workers are invoked again and terminal failures are not retried.
- `SWITCH`, dynamic-task, and fork/join paths select the intended branch.
- Workflow outputs contain the contract your callers depend on.
