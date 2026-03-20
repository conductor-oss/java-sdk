package dataanonymization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeneralizeDataWorkerTest {

    private final GeneralizeDataWorker worker = new GeneralizeDataWorker();

    @Test
    void taskDefName() {
        assertEquals("an_generalize_data", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void generalizesNameField() {
        List<Map<String, Object>> pii = List.of(
                Map.of("field", "name", "type", "direct_identifier", "action", "generalize"));
        List<Map<String, Object>> dataset = List.of(
                Map.of("name", "Alice Johnson", "email", "alice@company.com"));
        Task task = taskWith(Map.of("dataset", dataset, "piiFields", pii));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> generalized = (List<Map<String, Object>>) result.getOutputData().get("generalized");
        assertEquals("A***", generalized.get(0).get("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void generalizesAgeIntoBucket() {
        List<Map<String, Object>> pii = List.of(
                Map.of("field", "age", "type", "quasi_identifier", "action", "generalize"));
        List<Map<String, Object>> dataset = List.of(Map.of("age", 34));
        Task task = taskWith(Map.of("dataset", dataset, "piiFields", pii));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> generalized = (List<Map<String, Object>>) result.getOutputData().get("generalized");
        assertEquals("30-39", generalized.get(0).get("age"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void generalizesZipCode() {
        List<Map<String, Object>> pii = List.of(
                Map.of("field", "zipCode", "type", "quasi_identifier", "action", "generalize"));
        List<Map<String, Object>> dataset = List.of(Map.of("zipCode", "94103"));
        Task task = taskWith(Map.of("dataset", dataset, "piiFields", pii));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> generalized = (List<Map<String, Object>>) result.getOutputData().get("generalized");
        assertEquals("941**", generalized.get(0).get("zipCode"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("dataset", null);
        input.put("piiFields", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
