package datalineage.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyTransform2WorkerTest {

    private final ApplyTransform2Worker worker = new ApplyTransform2Worker();

    @Test
    void taskDefName() {
        assertEquals("ln_apply_transform_2", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void lowercasesEmails() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("email", "ALICE@EXAMPLE.COM"), Map.of("email", "BOB@TEST.COM")),
                "lineage", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("alice@example.com", records.get(0).get("email"));
        assertEquals("bob@test.com", records.get(1).get("email"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsTransformedFlag() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("email", "TEST@X.COM")),
                "lineage", List.of()));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> records = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(true, records.get(0).get("transformed_2"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void appendsLineageEntry() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("email", "a@b.com")),
                "lineage", List.of(Map.of("step", "source"))));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> lineage = (List<Map<String, Object>>) result.getOutputData().get("lineage");
        assertEquals(2, lineage.size());
        assertEquals("transform_2", lineage.get(1).get("step"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "lineage", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("lineage", null);
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
