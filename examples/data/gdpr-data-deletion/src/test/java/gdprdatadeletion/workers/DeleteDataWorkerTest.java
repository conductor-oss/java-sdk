package gdprdatadeletion.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeleteDataWorkerTest {

    private final DeleteDataWorker worker = new DeleteDataWorker();

    @Test
    void taskDefName() {
        assertEquals("gr_delete_data", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void deletesRecordsWhenVerified() {
        List<Map<String, Object>> records = List.of(
                Map.of("system", "user_accounts", "recordId", "USR-001"),
                Map.of("system", "analytics", "recordId", "EVT-1001"));
        Map<String, Object> input = new HashMap<>();
        input.put("records", records);
        input.put("verified", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("deletedCount"));
        List<Map<String, Object>> deleted = (List<Map<String, Object>>) result.getOutputData().get("deletedRecords");
        assertEquals(2, deleted.size());
        assertEquals("deleted", deleted.get(0).get("status"));
    }

    @Test
    void abortsWhenNotVerified() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of(Map.of("system", "billing", "recordId", "INV-001")));
        input.put("verified", false);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("deletedCount"));
    }

    @Test
    void returnsTimestampWhenDeleted() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of(Map.of("system", "support", "recordId", "TKT-001")));
        input.put("verified", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void returnsNullTimestampWhenAborted() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of(Map.of("system", "support", "recordId", "TKT-001")));
        input.put("verified", false);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void handlesEmptyRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of());
        input.put("verified", true);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("deletedCount"));
    }

    @Test
    void handlesMissingVerified() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", List.of(Map.of("system", "billing", "recordId", "INV-001")));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("deletedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
