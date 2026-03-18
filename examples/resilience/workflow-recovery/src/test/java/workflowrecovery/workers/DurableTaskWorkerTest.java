package workflowrecovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DurableTaskWorkerTest {

    private final DurableTaskWorker worker = new DurableTaskWorker();

    @BeforeEach
    void setUp() {
        DurableTaskWorker.clearCheckpoints();
    }

    @Test
    void taskDefName() {
        assertEquals("wr_durable_task", worker.getTaskDefName());
    }

    @Test
    void processesBatchAndCreatesCheckpoint() {
        Task task = taskWith(Map.of("batch", "batch-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("batch-001", result.getOutputData().get("batch"));
        assertEquals(false, result.getOutputData().get("resumed"));
        assertNotNull(result.getOutputData().get("checkpointId"));
        String checkpointId = (String) result.getOutputData().get("checkpointId");
        assertTrue(checkpointId.startsWith("chk-"), "Checkpoint ID should start with chk-");
    }

    @Test
    void resumesFromCheckpointOnSecondExecution() {
        // First execution creates checkpoint
        Task task1 = taskWith(Map.of("batch", "batch-002"));
        TaskResult result1 = worker.execute(task1);
        assertEquals(false, result1.getOutputData().get("resumed"));

        // Second execution should resume from checkpoint
        Task task2 = taskWith(Map.of("batch", "batch-002"));
        TaskResult result2 = worker.execute(task2);
        assertEquals(true, result2.getOutputData().get("resumed"));
        assertEquals(result1.getOutputData().get("checkpointId"),
                result2.getOutputData().get("checkpointId"));
    }

    @Test
    void differentBatchesAreIndependent() {
        Task taskA = taskWith(Map.of("batch", "batch-A"));
        Task taskB = taskWith(Map.of("batch", "batch-B"));

        TaskResult resultA = worker.execute(taskA);
        TaskResult resultB = worker.execute(taskB);

        assertEquals("batch-A", resultA.getOutputData().get("batch"));
        assertEquals("batch-B", resultB.getOutputData().get("batch"));
        assertEquals(false, resultA.getOutputData().get("resumed"));
        assertEquals(false, resultB.getOutputData().get("resumed"));
        assertNotEquals(resultA.getOutputData().get("checkpointId"),
                resultB.getOutputData().get("checkpointId"));
    }

    @Test
    void checkpointIdIsDeterministic() {
        Task task1 = taskWith(Map.of("batch", "batch-X"));
        TaskResult r1 = worker.execute(task1);
        String ckpt1 = (String) r1.getOutputData().get("checkpointId");

        DurableTaskWorker.clearCheckpoints();

        Task task2 = taskWith(Map.of("batch", "batch-X"));
        TaskResult r2 = worker.execute(task2);
        String ckpt2 = (String) r2.getOutputData().get("checkpointId");

        assertEquals(ckpt1, ckpt2, "Same batch should produce same checkpoint ID");
    }

    @Test
    void handlesEmptyBatch() {
        Task task = taskWith(Map.of("batch", ""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("", result.getOutputData().get("batch"));
    }

    @Test
    void handlesMissingBatchInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesNumericBatchId() {
        Task task = taskWith(Map.of("batch", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("42", result.getOutputData().get("batch"));
    }

    @Test
    void processedAtIsRealTimestamp() {
        Task task = taskWith(Map.of("batch", "batch-ts"));
        TaskResult result = worker.execute(task);

        String processedAt = (String) result.getOutputData().get("processedAt");
        assertNotNull(processedAt);
        assertTrue(processedAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z"),
                "processedAt should be ISO-8601, got: " + processedAt);
    }

    @Test
    void alwaysReturnsCompleted() {
        Task task = taskWith(Map.of("batch", "any-batch"));
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
