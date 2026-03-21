package humangroupclaim.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntakeWorkerTest {

    @Test
    void taskDefName() {
        IntakeWorker worker = new IntakeWorker();
        assertEquals("hgc_intake", worker.getTaskDefName());
    }

    @Test
    void returnsQueuedTrue() {
        IntakeWorker worker = new IntakeWorker();
        Task task = taskWith(Map.of("ticketId", "TKT-001", "assignedGroup", "support-tier-2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
    }

    @Test
    void completesWithEmptyInput() {
        IntakeWorker worker = new IntakeWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("queued"));
    }

    @Test
    void outputContainsQueuedKey() {
        IntakeWorker worker = new IntakeWorker();
        Task task = taskWith(Map.of("ticketId", "TKT-999"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("queued"));
    }

    @Test
    void deterministicOutput() {
        IntakeWorker worker = new IntakeWorker();
        Task task1 = taskWith(Map.of("ticketId", "TKT-001"));
        Task task2 = taskWith(Map.of("ticketId", "TKT-001"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData(), result2.getOutputData());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
