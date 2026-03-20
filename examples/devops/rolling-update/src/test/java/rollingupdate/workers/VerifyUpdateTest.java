package rollingupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VerifyUpdateTest {

    private final VerifyUpdate worker = new VerifyUpdate();

    @Test
    void taskDefName() {
        assertEquals("ru_verify", worker.getTaskDefName());
    }

    @Test
    void verifyCompletesSuccessfully() {
        Task task = taskWith(false);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verify"));
    }

    @Test
    void noRollbackMeansAllHealthy() {
        Task task = taskWith(false);
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("allHealthy"));
        assertEquals(false, result.getOutputData().get("rollbackOccurred"));
    }

    @Test
    void rollbackTriggeredMeansNotHealthy() {
        Task task = taskWith(true);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("allHealthy"));
        assertEquals(true, result.getOutputData().get("rollbackOccurred"));
    }

    @Test
    void outputContainsCompletedAtTimestamp() {
        Task task = taskWith(false);
        TaskResult result = worker.execute(task);

        String ts = (String) result.getOutputData().get("completedAt");
        assertNotNull(ts);
        assertTrue(ts.contains("T"));
    }

    @Test
    void verifyFlagAlwaysTrue() {
        Task task = taskWith(true);
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verify"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(true);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void nullVerifyDataDefaultsToHealthy() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allHealthy"));
        assertEquals(false, result.getOutputData().get("rollbackOccurred"));
    }

    private Task taskWith(boolean rollbackTriggered) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> verifyData = new HashMap<>();
        verifyData.put("rollbackTriggered", rollbackTriggered);
        Map<String, Object> input = new HashMap<>();
        input.put("verifyData", verifyData);
        task.setInputData(input);
        return task;
    }
}
