package artifactmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SignWorkerTest {

    private final SignWorker worker = new SignWorker();

    @Test
    void taskDefName() {
        assertEquals("am_sign", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsSignedFlag() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("sign"));
    }

    @Test
    void returnsProcessedFlag() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
