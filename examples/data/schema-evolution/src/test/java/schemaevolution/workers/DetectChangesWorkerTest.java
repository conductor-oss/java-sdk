package schemaevolution.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectChangesWorkerTest {

    private final DetectChangesWorker worker = new DetectChangesWorker();

    @Test
    void taskDefName() {
        assertEquals("sh_detect_changes", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void detectsFiveChanges() {
        Task task = taskWith(Map.of("currentSchema", Map.of("version", "v1"), "targetSchema", Map.of("version", "v2")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("changeCount"));
        List<Map<String, Object>> changes = (List<Map<String, Object>>) result.getOutputData().get("changes");
        assertEquals(5, changes.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsDistinctChangeTypes() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        List<String> changeTypes = (List<String>) result.getOutputData().get("changeTypes");
        assertNotNull(changeTypes);
        assertTrue(changeTypes.contains("ADD_FIELD"));
        assertTrue(changeTypes.contains("RENAME_FIELD"));
        assertTrue(changeTypes.contains("CHANGE_TYPE"));
        assertTrue(changeTypes.contains("DROP_FIELD"));
        assertTrue(changeTypes.contains("ADD_DEFAULT"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
