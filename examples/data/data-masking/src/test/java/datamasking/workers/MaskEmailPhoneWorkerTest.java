package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaskEmailPhoneWorkerTest {

    private final MaskEmailPhoneWorker worker = new MaskEmailPhoneWorker();

    @Test
    void taskDefName() {
        assertEquals("mk_mask_email_phone", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void masksEmail() {
        List<Map<String, Object>> records = List.of(
                new HashMap<>(Map.of("name", "Alice", "email", "alice@example.com")));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> masked = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("a***@example.com", masked.get(0).get("email"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void masksPhone() {
        List<Map<String, Object>> records = List.of(
                new HashMap<>(Map.of("name", "Alice", "phone", "555-123-4567")));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> masked = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("***-***-4567", masked.get(0).get("phone"));
    }

    @Test
    void countsBothEmailAndPhone() {
        List<Map<String, Object>> records = List.of(
                new HashMap<>(Map.of("name", "Alice", "email", "a@b.com", "phone", "555-1234")));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);
        assertEquals(2, result.getOutputData().get("maskedCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("maskedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
