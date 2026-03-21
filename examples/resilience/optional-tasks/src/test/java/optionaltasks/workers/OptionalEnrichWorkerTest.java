package optionaltasks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptionalEnrichWorkerTest {

    @Test
    void taskDefName() {
        OptionalEnrichWorker worker = new OptionalEnrichWorker();
        assertEquals("opt_optional_enrich", worker.getTaskDefName());
    }

    @Test
    void returnsEnrichedData() {
        OptionalEnrichWorker worker = new OptionalEnrichWorker();
        Task task = taskWith(Map.of("processedData", "processed-hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("extra-data-added", result.getOutputData().get("enriched"));
    }

    @Test
    void completesWithoutInput() {
        OptionalEnrichWorker worker = new OptionalEnrichWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("extra-data-added", result.getOutputData().get("enriched"));
    }

    @Test
    void outputContainsEnrichedKey() {
        OptionalEnrichWorker worker = new OptionalEnrichWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("enriched"));
    }

    @Test
    void resultIsDeterministic() {
        OptionalEnrichWorker worker = new OptionalEnrichWorker();
        Task task1 = taskWith(Map.of());
        Task task2 = taskWith(Map.of());
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("enriched"), result2.getOutputData().get("enriched"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
