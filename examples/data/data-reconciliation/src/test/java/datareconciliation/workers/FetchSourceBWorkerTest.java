package datareconciliation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FetchSourceBWorkerTest {

    private final FetchSourceBWorker worker = new FetchSourceBWorker();

    @Test
    void taskDefName() {
        assertEquals("rc_fetch_source_b", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetchesFiveRecords() {
        Task task = taskWith(Map.of("source", Map.of("system", "fulfillment")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("recordCount"));
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(5, records.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void containsDifferentRecordsThanSourceA() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        // Source B has ORD-006 but not ORD-004
        assertTrue(records.stream().anyMatch(r -> "ORD-006".equals(r.get("orderId"))));
        assertTrue(records.stream().noneMatch(r -> "ORD-004".equals(r.get("orderId"))));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
