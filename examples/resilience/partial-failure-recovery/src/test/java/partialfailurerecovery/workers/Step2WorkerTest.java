package partialfailurerecovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step2WorkerTest {

    @Test
    void taskDefName() {
        Step2Worker worker = new Step2Worker();
        assertEquals("pfr_step2", worker.getTaskDefName());
    }

    @Test
    void failsOnFirstAttempt() {
        Step2Worker worker = new Step2Worker();
        Task task = taskWith(Map.of("prev", "s1-hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void succeedsOnSecondAttempt() {
        Step2Worker worker = new Step2Worker();

        // First attempt — fails
        Task task1 = taskWith(Map.of("prev", "s1-hello"));
        TaskResult result1 = worker.execute(task1);
        assertEquals(TaskResult.Status.FAILED, result1.getStatus());

        // Second attempt — succeeds
        Task task2 = taskWith(Map.of("prev", "s1-hello"));
        TaskResult result2 = worker.execute(task2);
        assertEquals(TaskResult.Status.COMPLETED, result2.getStatus());
        assertEquals("s2-s1-hello", result2.getOutputData().get("result"));
    }

    @Test
    void resetAllowsRetesting() {
        Step2Worker worker = new Step2Worker();

        // First call fails
        Task task1 = taskWith(Map.of("prev", "s1-data"));
        TaskResult result1 = worker.execute(task1);
        assertEquals(TaskResult.Status.FAILED, result1.getStatus());

        // Reset the counter
        worker.reset();

        // After reset, first call fails again
        Task task2 = taskWith(Map.of("prev", "s1-data"));
        TaskResult result2 = worker.execute(task2);
        assertEquals(TaskResult.Status.FAILED, result2.getStatus());
    }

    @Test
    void handlesEmptyPrev() {
        Step2Worker worker = new Step2Worker();

        // First attempt — fails
        Task task1 = taskWith(Map.of());
        worker.execute(task1);

        // Second attempt — succeeds with empty prev
        Task task2 = taskWith(Map.of());
        TaskResult result2 = worker.execute(task2);
        assertEquals(TaskResult.Status.COMPLETED, result2.getStatus());
        assertEquals("s2-", result2.getOutputData().get("result"));
    }

    @Test
    void outputContainsErrorOnFailure() {
        Step2Worker worker = new Step2Worker();
        Task task = taskWith(Map.of("prev", "s1-hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("error"));
        assertFalse(result.getOutputData().containsKey("result"));
    }

    @Test
    void outputContainsResultOnSuccess() {
        Step2Worker worker = new Step2Worker();

        // Burn first attempt
        worker.execute(taskWith(Map.of("prev", "s1-x")));

        // Second attempt succeeds
        Task task = taskWith(Map.of("prev", "s1-x"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("result"));
        assertFalse(result.getOutputData().containsKey("error"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
