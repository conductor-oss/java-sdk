package escalationtimer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    @Test
    void taskDefName() {
        ProcessWorker worker = new ProcessWorker();
        assertEquals("et_process", worker.getTaskDefName());
    }

    @Test
    void processesAutoApprovedDecision() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("decision", "auto-approved", "method", "escalation-timer"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesManualApproval() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("decision", "approved", "method", "manual"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void processesRejection() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("decision", "rejected", "method", "manual"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesEmptyInput() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingDecision() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("method", "escalation-timer"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingMethod() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("decision", "auto-approved"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNonStringInputs() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("decision", 123, "method", 456));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputIsDeterministic() {
        ProcessWorker worker = new ProcessWorker();

        Task task1 = taskWith(Map.of("decision", "auto-approved", "method", "escalation-timer"));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("decision", "auto-approved", "method", "escalation-timer"));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getStatus(), result2.getStatus());
        assertEquals(result1.getOutputData().get("processed"), result2.getOutputData().get("processed"));
    }

    @Test
    void alwaysReturnsProcessedTrue() {
        ProcessWorker worker = new ProcessWorker();
        Task task = taskWith(Map.of("decision", "auto-approved", "method", "escalation-timer"));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
