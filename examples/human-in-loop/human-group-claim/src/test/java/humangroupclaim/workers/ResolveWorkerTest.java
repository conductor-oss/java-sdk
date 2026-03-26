package humangroupclaim.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResolveWorkerTest {

    @Test
    void taskDefName() {
        ResolveWorker worker = new ResolveWorker();
        assertEquals("hgc_resolve", worker.getTaskDefName());
    }

    @Test
    void returnsClosedTrue() {
        ResolveWorker worker = new ResolveWorker();
        Task task = taskWith(Map.of("ticketId", "TKT-001", "claimedBy", "agent-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("closed"));
    }

    @Test
    void completesWithEmptyInput() {
        ResolveWorker worker = new ResolveWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("closed"));
    }

    @Test
    void outputContainsClosedKey() {
        ResolveWorker worker = new ResolveWorker();
        Task task = taskWith(Map.of("ticketId", "TKT-999"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("closed"));
    }

    @Test
    void deterministicOutput() {
        ResolveWorker worker = new ResolveWorker();
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
