package dataexportrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {
    private final CollectDataWorker worker = new CollectDataWorker();

    @Test void taskDefName() { assertEquals("der_collect", worker.getTaskDefName()); }

    @Test void collectsData() {
        Task task = taskWith(Map.of("userId", "USR-123", "categories", List.of("profile", "activity")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1247, result.getOutputData().get("totalRecords"));
    }

    @Test void includesCollectedData() {
        Task task = taskWith(Map.of("userId", "USR-123", "categories", List.of("profile")));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("collectedData"));
    }

    @Test void handlesNullCategories() {
        Map<String, Object> input = new HashMap<>(); input.put("userId", "USR-123"); input.put("categories", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
