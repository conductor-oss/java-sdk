package schemaevolution.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyTransformWorkerTest {

    private final ApplyTransformWorker worker = new ApplyTransformWorker();

    @Test
    void taskDefName() {
        assertEquals("sh_apply_transform", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void appliesAllTransformations() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 1); record.put("name", "Alice"); record.put("phone", "555-0101");
        record.put("age", "30"); record.put("legacy_id", "L001");

        Task task = taskWith(Map.of("sampleData", List.of(record)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> transformed = (List<Map<String, Object>>) result.getOutputData().get("transformedData");
        assertEquals(1, transformed.size());

        Map<String, Object> t = transformed.get(0);
        assertNull(t.get("middle_name"));
        assertEquals("555-0101", t.get("phone_number"));
        assertFalse(t.containsKey("phone"));
        assertEquals(30, t.get("age"));
        assertFalse(t.containsKey("legacy_id"));
        assertEquals("active", t.get("status"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("sampleData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
