package exponentialmaxretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeadLetterLogWorkerTest {

    @Test
    void taskDefName() {
        DeadLetterLogWorker worker = new DeadLetterLogWorker();
        assertEquals("emr_dead_letter_log", worker.getTaskDefName());
    }

    @Test
    void logsFailedWorkflowDetails() {
        DeadLetterLogWorker worker = new DeadLetterLogWorker();
        Task task = taskWith(Map.of(
                "failedWorkflowId", "wf-12345",
                "reason", "Max retries exceeded"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("wf-12345", result.getOutputData().get("failedWorkflowId"));
        assertEquals("Max retries exceeded", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullFailedWorkflowId() {
        DeadLetterLogWorker worker = new DeadLetterLogWorker();
        Task task = taskWith(Map.of("reason", "Some error"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("unknown", result.getOutputData().get("failedWorkflowId"));
        assertEquals("Some error", result.getOutputData().get("reason"));
    }

    @Test
    void handlesNullReason() {
        DeadLetterLogWorker worker = new DeadLetterLogWorker();
        Task task = taskWith(Map.of("failedWorkflowId", "wf-99999"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("wf-99999", result.getOutputData().get("failedWorkflowId"));
        assertEquals("unknown", result.getOutputData().get("reason"));
    }

    @Test
    void handlesEmptyInput() {
        DeadLetterLogWorker worker = new DeadLetterLogWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("unknown", result.getOutputData().get("failedWorkflowId"));
        assertEquals("unknown", result.getOutputData().get("reason"));
    }

    @Test
    void alwaysReturnsCompleted() {
        DeadLetterLogWorker worker = new DeadLetterLogWorker();
        Task task = taskWith(Map.of(
                "failedWorkflowId", "wf-abc",
                "reason", "timeout"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsLoggedFlag() {
        DeadLetterLogWorker worker = new DeadLetterLogWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("logged"));
        assertEquals(true, result.getOutputData().get("logged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
