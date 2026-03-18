package deadletter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandleFailureWorkerTest {

    @Test
    void taskDefName() {
        HandleFailureWorker worker = new HandleFailureWorker();
        assertEquals("dl_handle_failure", worker.getTaskDefName());
    }

    @Test
    void handlesFailureWithAllInputs() {
        HandleFailureWorker worker = new HandleFailureWorker();
        Task task = taskWith(Map.of(
                "failedWorkflowId", "wf-123",
                "failedTaskName", "dl_process",
                "error", "Processing failed for data: order-456"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
        assertEquals("wf-123", result.getOutputData().get("failedWorkflowId"));
        assertEquals("dl_process", result.getOutputData().get("failedTaskName"));
        assertEquals("Processing failed for data: order-456", result.getOutputData().get("error"));
        assertTrue(((String) result.getOutputData().get("summary")).contains("wf-123"));
        assertTrue(((String) result.getOutputData().get("summary")).contains("dl_process"));
    }

    @Test
    void handlesEmptyInputs() {
        HandleFailureWorker worker = new HandleFailureWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
        assertEquals("", result.getOutputData().get("failedWorkflowId"));
        assertEquals("", result.getOutputData().get("failedTaskName"));
        assertEquals("", result.getOutputData().get("error"));
    }

    @Test
    void outputContainsHandledFlag() {
        HandleFailureWorker worker = new HandleFailureWorker();
        Task task = taskWith(Map.of(
                "failedWorkflowId", "wf-abc",
                "failedTaskName", "some_task",
                "error", "some error"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("handled"));
        assertEquals(true, result.getOutputData().get("handled"));
    }

    @Test
    void outputContainsSummary() {
        HandleFailureWorker worker = new HandleFailureWorker();
        Task task = taskWith(Map.of(
                "failedWorkflowId", "wf-xyz",
                "failedTaskName", "dl_process",
                "error", "timeout error"));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.contains("wf-xyz"));
        assertTrue(summary.contains("dl_process"));
        assertTrue(summary.contains("timeout error"));
    }

    @Test
    void handlesNonStringInputsGracefully() {
        HandleFailureWorker worker = new HandleFailureWorker();
        Task task = taskWith(Map.of(
                "failedWorkflowId", 123,
                "failedTaskName", 456,
                "error", 789));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("handled"));
        assertEquals("", result.getOutputData().get("failedWorkflowId"));
        assertEquals("", result.getOutputData().get("failedTaskName"));
        assertEquals("", result.getOutputData().get("error"));
    }

    @Test
    void alwaysReturnsCompleted() {
        HandleFailureWorker worker = new HandleFailureWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
