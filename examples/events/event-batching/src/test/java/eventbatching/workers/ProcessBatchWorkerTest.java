package eventbatching.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessBatchWorkerTest {

    private final ProcessBatchWorker worker = new ProcessBatchWorker();

    @Test
    void taskDefName() {
        assertEquals("eb_process_batch", worker.getTaskDefName());
    }

    @Test
    void processesFirstBatch() {
        Task task = taskWith(Map.of(
                "batches", twoBatches(),
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("batchIndex"));
        assertEquals(3, result.getOutputData().get("eventsProcessed"));
    }

    @Test
    void processesSecondBatch() {
        Task task = taskWith(Map.of(
                "batches", twoBatches(),
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("batchIndex"));
        assertEquals(3, result.getOutputData().get("eventsProcessed"));
    }

    @Test
    void batchIndexMatchesIteration() {
        Task task = taskWith(Map.of(
                "batches", twoBatches(),
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("batchIndex"));
    }

    @Test
    void eventsProcessedMatchesBatchSize() {
        Task task = taskWith(Map.of(
                "batches", twoBatches(),
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("eventsProcessed"));
    }

    @Test
    void handlesOutOfBoundsIteration() {
        Task task = taskWith(Map.of(
                "batches", twoBatches(),
                "iteration", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("batchIndex"));
        assertEquals(0, result.getOutputData().get("eventsProcessed"));
    }

    @Test
    void handlesNullBatches() {
        Map<String, Object> input = new HashMap<>();
        input.put("batches", null);
        input.put("iteration", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("eventsProcessed"));
    }

    @Test
    void handlesDefaultIterationZero() {
        Task task = taskWith(Map.of(
                "batches", twoBatches()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("batchIndex"));
        assertEquals(3, result.getOutputData().get("eventsProcessed"));
    }

    @Test
    void handlesSmallerBatch() {
        List<List<Map<String, Object>>> batches = List.of(
                List.of(Map.of("id", "e1", "type", "click"),
                        Map.of("id", "e2", "type", "view")));
        Task task = taskWith(Map.of(
                "batches", batches,
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("eventsProcessed"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("batchIndex"));
        assertEquals(0, result.getOutputData().get("eventsProcessed"));
    }

    private List<List<Map<String, Object>>> twoBatches() {
        return List.of(
                List.of(Map.of("id", "e1", "type", "click"),
                        Map.of("id", "e2", "type", "view"),
                        Map.of("id", "e3", "type", "click")),
                List.of(Map.of("id", "e4", "type", "purchase"),
                        Map.of("id", "e5", "type", "view"),
                        Map.of("id", "e6", "type", "click")));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
