package gracefuldegradation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    private final FinalizeWorker worker = new FinalizeWorker();

    @Test
    void taskDefName() {
        assertEquals("gd_finalize", worker.getTaskDefName());
    }

    @Test
    void bothServicesAvailable_notDegraded() {
        Task task = taskWith(Map.of("enriched", true, "analytics", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enriched"));
        assertEquals(true, result.getOutputData().get("analytics"));
        assertEquals(false, result.getOutputData().get("degraded"));
    }

    @Test
    void enrichmentDown_isDegraded() {
        Task task = taskWith(Map.of("enriched", false, "analytics", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("enriched"));
        assertEquals(true, result.getOutputData().get("analytics"));
        assertEquals(true, result.getOutputData().get("degraded"));
    }

    @Test
    void analyticsDown_isDegraded() {
        Task task = taskWith(Map.of("enriched", true, "analytics", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enriched"));
        assertEquals(false, result.getOutputData().get("analytics"));
        assertEquals(true, result.getOutputData().get("degraded"));
    }

    @Test
    void bothServicesDown_isDegraded() {
        Task task = taskWith(Map.of("enriched", false, "analytics", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("enriched"));
        assertEquals(false, result.getOutputData().get("analytics"));
        assertEquals(true, result.getOutputData().get("degraded"));
    }

    @Test
    void missingInputs_defaultsToFalseAndDegraded() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("enriched"));
        assertEquals(false, result.getOutputData().get("analytics"));
        assertEquals(true, result.getOutputData().get("degraded"));
    }

    @Test
    void nullInputs_defaultsToFalseAndDegraded() {
        Map<String, Object> input = new HashMap<>();
        input.put("enriched", null);
        input.put("analytics", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("enriched"));
        assertEquals(false, result.getOutputData().get("analytics"));
        assertEquals(true, result.getOutputData().get("degraded"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("enriched", true, "analytics", true));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("enriched"));
        assertTrue(result.getOutputData().containsKey("analytics"));
        assertTrue(result.getOutputData().containsKey("degraded"));
        assertEquals(3, result.getOutputData().size());
    }

    @Test
    void stringBooleanInputsAreHandled() {
        Task task = taskWith(Map.of("enriched", "true", "analytics", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enriched"));
        assertEquals(false, result.getOutputData().get("analytics"));
        assertEquals(true, result.getOutputData().get("degraded"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
