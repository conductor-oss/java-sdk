package multilevelapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubmitWorkerTest {

    @Test
    void taskDefName() {
        SubmitWorker worker = new SubmitWorker();
        assertEquals("mla_submit", worker.getTaskDefName());
    }

    @Test
    void returnsSubmittedTrue() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(Map.of("requestId", "REQ-001", "requestor", "alice"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void completesWithEmptyInput() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void outputContainsSubmittedKey() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(Map.of("requestId", "REQ-999"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("submitted"));
    }

    @Test
    void deterministicOutput() {
        SubmitWorker worker = new SubmitWorker();
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
