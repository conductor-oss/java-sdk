package multilevelapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    @Test
    void taskDefName() {
        FinalizeWorker worker = new FinalizeWorker();
        assertEquals("mla_finalize", worker.getTaskDefName());
    }

    @Test
    void returnsFinalizedTrue() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of(
                "requestId", "REQ-001",
                "requestor", "alice",
                "managerApproval", "true",
                "directorApproval", "true",
                "vpApproval", "true"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void completesWithEmptyInput() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("finalized"));
    }

    @Test
    void outputContainsFinalizedKey() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of("requestId", "REQ-123"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("finalized"));
    }

    @Test
    void deterministicOutput() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task1 = taskWith(Map.of("requestId", "REQ-001"));
        Task task2 = taskWith(Map.of("requestId", "REQ-001"));

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
