package dataanonymization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyAnonymizationWorkerTest {

    private final VerifyAnonymizationWorker worker = new VerifyAnonymizationWorker();

    @Test
    void taskDefName() {
        assertEquals("an_verify_anonymization", worker.getTaskDefName());
    }

    @Test
    void verifiesWhenAllDirectIdsAnonymized() {
        List<Map<String, Object>> data = List.of(
                Map.of("name", "A***", "email", "[REDACTED]", "ssn", "[REDACTED]", "phone", "[REDACTED]"));
        List<Map<String, Object>> piiFields = List.of(
                Map.of("field", "name", "type", "direct_identifier"),
                Map.of("field", "email", "type", "direct_identifier"),
                Map.of("field", "ssn", "type", "direct_identifier"),
                Map.of("field", "phone", "type", "direct_identifier"));
        Task task = taskWith(Map.of("anonymizedData", data, "originalPiiFields", piiFields));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(3, result.getOutputData().get("kAnonymity"));
        assertEquals(1, result.getOutputData().get("recordCount"));
    }

    @Test
    void failsWhenDirectIdNotAnonymized() {
        List<Map<String, Object>> data = List.of(
                Map.of("name", "Alice Johnson", "email", "[REDACTED]", "ssn", "[REDACTED]", "phone", "[REDACTED]"));
        List<Map<String, Object>> piiFields = List.of(
                Map.of("field", "name", "type", "direct_identifier"),
                Map.of("field", "email", "type", "direct_identifier"));
        Task task = taskWith(Map.of("anonymizedData", data, "originalPiiFields", piiFields));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test
    void handlesEmptyData() {
        Task task = taskWith(Map.of("anonymizedData", List.of(), "originalPiiFields", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
