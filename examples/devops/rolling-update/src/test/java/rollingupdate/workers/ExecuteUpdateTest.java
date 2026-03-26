package rollingupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteUpdateTest {

    private final ExecuteUpdate worker = new ExecuteUpdate();

    @Test
    void taskDefName() {
        assertEquals("ru_execute", worker.getTaskDefName());
    }

    @Test
    void executeCompletesSuccessfully() {
        Task task = taskWith(5);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("execute"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void allBatchesCompleted() {
        Task task = taskWith(5);
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("batchesCompleted"));
        assertEquals(5, result.getOutputData().get("totalBatches"));
    }

    @Test
    void noRollbackTriggered() {
        Task task = taskWith(3);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("rollbackTriggered"));
    }

    @Test
    void outputContainsTimestamp() {
        Task task = taskWith(3);
        TaskResult result = worker.execute(task);

        String ts = (String) result.getOutputData().get("updatedAt");
        assertNotNull(ts);
        assertTrue(ts.contains("T"));
    }

    @Test
    void threeBatchesForThreeReplicas() {
        Task task = taskWith(3);
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("batchesCompleted"));
    }

    @Test
    void singleBatchForSingleReplica() {
        Task task = taskWith(1);
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("batchesCompleted"));
    }

    @Test
    void nullExecuteDataDefaultsToThreeBatches() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("totalBatches"));
    }

    private Task taskWith(int totalBatches) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> executeData = new HashMap<>();
        executeData.put("totalBatches", totalBatches);
        Map<String, Object> input = new HashMap<>();
        input.put("executeData", executeData);
        task.setInputData(input);
        return task;
    }
}
