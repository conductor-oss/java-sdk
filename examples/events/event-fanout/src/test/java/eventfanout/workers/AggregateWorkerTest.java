package eventfanout.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateWorkerTest {

    private final AggregateWorker worker = new AggregateWorker();

    @Test
    void taskDefName() {
        assertEquals("fo_aggregate", worker.getTaskDefName());
    }

    @Test
    void aggregatesAllThreeResults() {
        Task task = taskWith(Map.of(
                "analyticsResult", "tracked",
                "storageResult", "stored",
                "notifyResult", "notified"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_completed", result.getOutputData().get("status"));

        @SuppressWarnings("unchecked")
        List<Object> processorResults = (List<Object>) result.getOutputData().get("processorResults");
        assertEquals(3, processorResults.size());
        assertEquals("tracked", processorResults.get(0));
        assertEquals("stored", processorResults.get(1));
        assertEquals("notified", processorResults.get(2));
    }

    @Test
    void outputStatusIsAllCompleted() {
        Task task = taskWith(Map.of(
                "analyticsResult", "tracked",
                "storageResult", "stored",
                "notifyResult", "notified"));
        TaskResult result = worker.execute(task);

        assertEquals("all_completed", result.getOutputData().get("status"));
    }

    @Test
    void processorResultsContainsThreeEntries() {
        Task task = taskWith(Map.of(
                "analyticsResult", "tracked",
                "storageResult", "stored",
                "notifyResult", "notified"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Object> processorResults = (List<Object>) result.getOutputData().get("processorResults");
        assertEquals(3, processorResults.size());
    }

    @Test
    void handlesNullAnalyticsResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("analyticsResult", null);
        input.put("storageResult", "stored");
        input.put("notifyResult", "notified");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_completed", result.getOutputData().get("status"));

        @SuppressWarnings("unchecked")
        List<Object> processorResults = (List<Object>) result.getOutputData().get("processorResults");
        assertEquals("none", processorResults.get(0));
        assertEquals("stored", processorResults.get(1));
        assertEquals("notified", processorResults.get(2));
    }

    @Test
    void handlesNullStorageResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("analyticsResult", "tracked");
        input.put("storageResult", null);
        input.put("notifyResult", "notified");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Object> processorResults = (List<Object>) result.getOutputData().get("processorResults");
        assertEquals("tracked", processorResults.get(0));
        assertEquals("none", processorResults.get(1));
        assertEquals("notified", processorResults.get(2));
    }

    @Test
    void handlesNullNotifyResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("analyticsResult", "tracked");
        input.put("storageResult", "stored");
        input.put("notifyResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Object> processorResults = (List<Object>) result.getOutputData().get("processorResults");
        assertEquals("tracked", processorResults.get(0));
        assertEquals("stored", processorResults.get(1));
        assertEquals("none", processorResults.get(2));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("all_completed", result.getOutputData().get("status"));

        @SuppressWarnings("unchecked")
        List<Object> processorResults = (List<Object>) result.getOutputData().get("processorResults");
        assertEquals(3, processorResults.size());
        assertEquals("none", processorResults.get(0));
        assertEquals("none", processorResults.get(1));
        assertEquals("none", processorResults.get(2));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
