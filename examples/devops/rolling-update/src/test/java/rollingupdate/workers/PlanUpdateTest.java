package rollingupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlanUpdateTest {

    private final PlanUpdate worker = new PlanUpdate();

    @Test
    void taskDefName() {
        assertEquals("ru_plan", worker.getTaskDefName());
    }

    @Test
    void planIsCreatedSuccessfully() {
        Task task = taskWith(5);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("plan"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void strategyIsRolling() {
        Task task = taskWith(5);
        TaskResult result = worker.execute(task);

        assertEquals("rolling", result.getOutputData().get("strategy"));
    }

    @Test
    void batchSizeIsOne() {
        Task task = taskWith(5);
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("batchSize"));
    }

    @Test
    void totalBatchesMatchesReplicas() {
        Task task = taskWith(5);
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("totalBatches"));
    }

    @Test
    void maxUnavailableIsCalculated() {
        Task task = taskWith(10);
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("maxUnavailable"));
    }

    @Test
    void rollbackOnFailureEnabled() {
        Task task = taskWith(3);
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("rollbackOnFailure"));
    }

    @Test
    void nullPlanDataDefaultsToThreeReplicas() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("totalBatches"));
    }

    private Task taskWith(int currentReplicas) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> planData = new HashMap<>();
        planData.put("currentReplicas", currentReplicas);
        Map<String, Object> input = new HashMap<>();
        input.put("planData", planData);
        task.setInputData(input);
        return task;
    }
}
