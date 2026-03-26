package dataanonymization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SuppressFieldsWorkerTest {

    private final SuppressFieldsWorker worker = new SuppressFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("an_suppress_fields", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void suppressesEmailSsnPhone() {
        List<Map<String, Object>> data = List.of(
                Map.of("name", "A***", "email", "alice@company.com", "ssn", "123-45-6789", "phone", "555-0101", "purchases", 12));
        Task task = taskWith(Map.of("generalizedData", data));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("suppressedCount"));

        List<Map<String, Object>> suppressed = (List<Map<String, Object>>) result.getOutputData().get("suppressed");
        assertEquals("[REDACTED]", suppressed.get(0).get("email"));
        assertEquals("[REDACTED]", suppressed.get(0).get("ssn"));
        assertEquals("[REDACTED]", suppressed.get(0).get("phone"));
        assertEquals("A***", suppressed.get(0).get("name"));
        assertEquals(12, suppressed.get(0).get("purchases"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("generalizedData", null);
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
