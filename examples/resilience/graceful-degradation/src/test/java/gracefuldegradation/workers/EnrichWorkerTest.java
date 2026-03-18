package gracefuldegradation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichWorkerTest {

    private final EnrichWorker worker = new EnrichWorker();

    @Test
    void taskDefName() {
        assertEquals("gd_enrich", worker.getTaskDefName());
    }

    @Test
    void availableTrue_returnsEnrichedTrue() {
        Task task = taskWith(Map.of("available", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enriched"));
    }

    @Test
    void availableFalse_returnsEnrichedFalse() {
        Task task = taskWith(Map.of("available", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("enriched"));
    }

    @Test
    void noAvailableInput_defaultsToTrue() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enriched"));
    }

    @Test
    void nullAvailableInput_defaultsToTrue() {
        Map<String, Object> input = new HashMap<>();
        input.put("available", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enriched"));
    }

    @Test
    void stringTrueIsAccepted() {
        Task task = taskWith(Map.of("available", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enriched"));
    }

    @Test
    void stringFalseIsAccepted() {
        Task task = taskWith(Map.of("available", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("enriched"));
    }

    @Test
    void outputContainsOnlyEnrichedKey() {
        Task task = taskWith(Map.of("available", true));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("enriched"));
        assertEquals(1, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
