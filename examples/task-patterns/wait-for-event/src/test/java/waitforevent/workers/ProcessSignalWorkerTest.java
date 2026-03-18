package waitforevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessSignalWorkerTest {

    private final ProcessSignalWorker worker = new ProcessSignalWorker();

    @Test
    void taskDefName() {
        assertEquals("we_process_signal", worker.getTaskDefName());
    }

    @Test
    void processesApprovedDecision() {
        Task task = taskWith(Map.of(
                "requestId", "REQ-001",
                "signalData", "external-approval-data",
                "decision", "approved"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("REQ-001", result.getOutputData().get("requestId"));
        assertEquals("approved", result.getOutputData().get("decision"));
    }

    @Test
    void processesRejectedDecision() {
        Task task = taskWith(Map.of(
                "requestId", "REQ-002",
                "signalData", "rejection-reason",
                "decision", "rejected"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("rejected", result.getOutputData().get("decision"));
    }

    @Test
    void handlesNullDecision() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", "REQ-003");
        input.put("signalData", "some-data");
        input.put("decision", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertNull(result.getOutputData().get("decision"));
    }

    @Test
    void handlesNullSignalData() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", "REQ-004");
        input.put("signalData", null);
        input.put("decision", "approved");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("approved", result.getOutputData().get("decision"));
    }

    @Test
    void handlesNullRequestId() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", null);
        input.put("signalData", "data");
        input.put("decision", "approved");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertNull(result.getOutputData().get("requestId"));
    }

    @Test
    void outputContainsProcessedFlag() {
        Task task = taskWith(Map.of(
                "requestId", "REQ-005",
                "signalData", "data",
                "decision", "approved"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("processed"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of(
                "requestId", "REQ-006",
                "signalData", "data",
                "decision", "denied"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void preservesRequestIdInOutput() {
        Task task = taskWith(Map.of(
                "requestId", "REQ-LONG-ID-12345",
                "signalData", "data",
                "decision", "approved"));
        TaskResult result = worker.execute(task);

        assertEquals("REQ-LONG-ID-12345", result.getOutputData().get("requestId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
