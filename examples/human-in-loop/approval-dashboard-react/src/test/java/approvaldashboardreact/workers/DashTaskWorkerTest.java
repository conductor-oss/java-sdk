package approvaldashboardreact.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DashTaskWorkerTest {

    @Test
    void taskDefName() {
        DashTaskWorker worker = new DashTaskWorker();
        assertEquals("dash_task", worker.getTaskDefName());
    }

    @Test
    void returnsProcessedTrue() {
        DashTaskWorker worker = new DashTaskWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "title", "Expense Report",
                "priority", "high",
                "assignee", "alice@example.com"
        )));
        task.setWorkflowInstanceId("wf-test-1");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void returnsCompletedStatusWithEmptyInput() {
        DashTaskWorker worker = new DashTaskWorker();
        Task task = taskWith(new HashMap<>());
        task.setWorkflowInstanceId("wf-empty-input");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsProcessedKey() {
        DashTaskWorker worker = new DashTaskWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "title", "Travel Request",
                "priority", "medium",
                "assignee", "bob@example.com"
        )));
        task.setWorkflowInstanceId("wf-output-check");

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
    }

    @Test
    void processedValueIsBoolean() {
        DashTaskWorker worker = new DashTaskWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "title", "License Request",
                "priority", "low",
                "assignee", "charlie@example.com"
        )));
        task.setWorkflowInstanceId("wf-boolean-check");

        TaskResult result = worker.execute(task);

        assertInstanceOf(Boolean.class, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNullWorkflowInstanceId() {
        DashTaskWorker worker = new DashTaskWorker();
        Task task = taskWith(new HashMap<>());
        // workflowInstanceId is null by default

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void multipleExecutionsAreIndependent() {
        DashTaskWorker worker = new DashTaskWorker();

        Task task1 = taskWith(new HashMap<>(Map.of("title", "First")));
        task1.setWorkflowInstanceId("wf-multi-1");
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(new HashMap<>(Map.of("title", "Second")));
        task2.setWorkflowInstanceId("wf-multi-2");
        TaskResult result2 = worker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result1.getStatus());
        assertEquals(TaskResult.Status.COMPLETED, result2.getStatus());
        assertEquals(true, result1.getOutputData().get("processed"));
        assertEquals(true, result2.getOutputData().get("processed"));
    }

    @Test
    void handlesHighPriorityInput() {
        DashTaskWorker worker = new DashTaskWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "title", "Urgent Approval",
                "priority", "high",
                "assignee", "admin@example.com"
        )));
        task.setWorkflowInstanceId("wf-high-priority");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
