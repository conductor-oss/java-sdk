package selfhealing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetryProcessWorkerTest {

    @Test
    void taskDefName() {
        RetryProcessWorker worker = new RetryProcessWorker();
        assertEquals("sh_retry_process", worker.getTaskDefName());
    }

    @Test
    void returnsHealedResult() {
        RetryProcessWorker worker = new RetryProcessWorker();
        Task task = taskWith(Map.of("data", "payload-2"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("healed-payload-2", result.getOutputData().get("result"));
    }

    @Test
    void handlesEmptyData() {
        RetryProcessWorker worker = new RetryProcessWorker();
        Task task = taskWith(Map.of("data", ""));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("healed-", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingData() {
        RetryProcessWorker worker = new RetryProcessWorker();
        Task task = taskWith(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("healed-", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
