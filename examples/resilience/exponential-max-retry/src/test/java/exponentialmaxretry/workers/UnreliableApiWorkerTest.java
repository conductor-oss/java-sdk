package exponentialmaxretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnreliableApiWorkerTest {

    @Test
    void taskDefName() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        assertEquals("emr_unreliable_api", worker.getTaskDefName());
    }

    @Test
    void succeedsWhenShouldSucceedIsTrue() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        Task task = taskWith(Map.of("shouldSucceed", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("API call successful", result.getOutputData().get("status"));
        assertEquals("processed", result.getOutputData().get("data"));
    }

    @Test
    void succeedsWhenShouldSucceedIsStringTrue() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        Task task = taskWith(Map.of("shouldSucceed", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("API call successful", result.getOutputData().get("status"));
        assertEquals("processed", result.getOutputData().get("data"));
    }

    @Test
    void failsWhenShouldSucceedIsFalse() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        Task task = taskWith(Map.of("shouldSucceed", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("API call failed", result.getOutputData().get("error"));
        assertEquals("FAILED", result.getOutputData().get("status"));
    }

    @Test
    void failsWhenShouldSucceedIsStringFalse() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        Task task = taskWith(Map.of("shouldSucceed", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("API call failed", result.getOutputData().get("error"));
    }

    @Test
    void failsWhenShouldSucceedIsAbsent() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("API call failed", result.getOutputData().get("error"));
    }

    @Test
    void outputContainsStatusOnSuccess() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        Task task = taskWith(Map.of("shouldSucceed", true));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("status"));
        assertTrue(result.getOutputData().containsKey("data"));
    }

    @Test
    void outputContainsErrorOnFailure() {
        UnreliableApiWorker worker = new UnreliableApiWorker();
        Task task = taskWith(Map.of("shouldSucceed", false));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("error"));
        assertTrue(result.getOutputData().containsKey("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
