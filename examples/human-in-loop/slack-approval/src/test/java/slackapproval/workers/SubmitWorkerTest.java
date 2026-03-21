package slackapproval.workers;

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
        assertEquals("sa_submit", worker.getTaskDefName());
    }

    @Test
    void returnsSubmittedTrue() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(new HashMap<>(Map.of("requestor", "alice@example.com", "reason", "Need access")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void completesWithEmptyInput() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void outputContainsSubmittedKey() {
        SubmitWorker worker = new SubmitWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("submitted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
