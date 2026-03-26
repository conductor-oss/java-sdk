package gitopsworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanSyncWorkerTest {

    private final PlanSyncWorker worker = new PlanSyncWorker();

    @Test
    void taskDefName() {
        assertEquals("go_plan_sync", worker.getTaskDefName());
    }

    @Test
    void plansSyncSuccessfully() {
        Task task = taskWith(Map.of("plan_syncData", Map.of("detect_driftId", "DETECT_DRIFT-1361")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("plan_sync"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsUpdateCount() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("updates"));
    }

    @Test
    void outputContainsCreateCount() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("creates"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void alwaysReturnsProcessed() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void alwaysReturnsPlanSync() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("plan_sync"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
