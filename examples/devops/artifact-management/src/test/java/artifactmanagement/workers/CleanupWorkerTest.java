package artifactmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CleanupWorkerTest {

    private final CleanupWorker worker = new CleanupWorker();

    @Test
    void taskDefName() {
        assertEquals("am_cleanup", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsCleanupFlag() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("cleanup"));
    }

    @Test
    void returnsCompletedAt() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("completedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
