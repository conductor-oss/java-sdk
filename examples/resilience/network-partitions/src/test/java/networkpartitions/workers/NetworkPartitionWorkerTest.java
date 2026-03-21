package networkpartitions.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkPartitionWorkerTest {

    @Test
    void taskDefName() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        assertEquals("np_resilient_task", worker.getTaskDefName());
    }

    @Test
    void processesTaskAndReturnsResult() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        Task task = taskWith(Map.of("data", "hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-hello", result.getOutputData().get("result"));
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void incrementsAttemptCountOnEachCall() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();

        Task task1 = taskWith(Map.of("data", "first"));
        TaskResult result1 = worker.execute(task1);
        assertEquals(1, result1.getOutputData().get("attempt"));

        Task task2 = taskWith(Map.of("data", "second"));
        TaskResult result2 = worker.execute(task2);
        assertEquals(2, result2.getOutputData().get("attempt"));

        assertEquals(2, worker.getAttemptCount());
    }

    @Test
    void resetClearsAttemptCounter() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();

        Task task = taskWith(Map.of("data", "test"));
        worker.execute(task);
        assertEquals(1, worker.getAttemptCount());

        worker.reset();
        assertEquals(0, worker.getAttemptCount());

        TaskResult result = worker.execute(taskWith(Map.of("data", "after-reset")));
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void handlesEmptyData() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-", result.getOutputData().get("result"));
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void handlesNullDataGracefully() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("done-", result.getOutputData().get("result"));
        assertEquals(1, result.getOutputData().get("attempt"));
    }

    @Test
    void resultContainsBothFields() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        Task task = taskWith(Map.of("data", "payload"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("attempt"));
    }

    @Test
    void getAttemptCountReturnsCurrentCount() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        assertEquals(0, worker.getAttemptCount());

        worker.execute(taskWith(Map.of("data", "a")));
        assertEquals(1, worker.getAttemptCount());

        worker.execute(taskWith(Map.of("data", "b")));
        worker.execute(taskWith(Map.of("data", "c")));
        assertEquals(3, worker.getAttemptCount());
    }

    @Test
    void dataValueIncludedInResult() {
        NetworkPartitionWorker worker = new NetworkPartitionWorker();
        Task task = taskWith(Map.of("data", "my-payload-123"));
        TaskResult result = worker.execute(task);

        assertEquals("done-my-payload-123", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
