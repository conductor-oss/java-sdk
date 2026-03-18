package csvprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformFieldsWorkerTest {

    private final TransformFieldsWorker worker = new TransformFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("cv_transform_fields", worker.getTaskDefName());
    }

    @Test
    void transformsNameToTrimmedFullName() {
        Task task = taskWith(Map.of(
                "validRows", List.of(
                        Map.of("name", "  Alice  ", "email", "alice@corp.com", "dept", "engineering", "salary", "95000")),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertEquals("Alice", rows.get(0).get("fullName"));
    }

    @Test
    void lowercasesEmail() {
        Task task = taskWith(Map.of(
                "validRows", List.of(
                        Map.of("name", "Bob", "email", "BOB@Corp.COM", "dept", "sales", "salary", "82000")),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertEquals("bob@corp.com", rows.get(0).get("emailAddress"));
    }

    @Test
    void uppercasesDepartment() {
        Task task = taskWith(Map.of(
                "validRows", List.of(
                        Map.of("name", "Alice", "email", "alice@corp.com", "dept", "engineering", "salary", "95000")),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertEquals("ENGINEERING", rows.get(0).get("department"));
    }

    @Test
    void parsesSalaryToDouble() {
        Task task = taskWith(Map.of(
                "validRows", List.of(
                        Map.of("name", "Alice", "email", "alice@corp.com", "dept", "eng", "salary", "95000.50")),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertEquals(95000.50, (double) rows.get(0).get("salary"), 0.01);
    }

    @Test
    void defaultsDeptToUnknownUppercased() {
        Map<String, Object> row = new HashMap<>();
        row.put("name", "Charlie");
        row.put("email", "charlie@corp.com");
        row.put("dept", null);
        row.put("salary", "70000");
        Task task = taskWith(Map.of(
                "validRows", List.of(row),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertEquals("UNKNOWN", rows.get(0).get("department"));
    }

    @Test
    void defaultsSalaryToZeroForInvalidString() {
        Task task = taskWith(Map.of(
                "validRows", List.of(
                        Map.of("name", "Alice", "email", "alice@corp.com", "dept", "hr", "salary", "not-a-number")),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOutputData().get("rows");
        assertEquals(0.0, (double) rows.get(0).get("salary"), 0.01);
    }

    @Test
    void handlesNullValidRows() {
        Map<String, Object> input = new HashMap<>();
        input.put("validRows", null);
        input.put("headers", List.of("name", "email"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void returnsCorrectCount() {
        Task task = taskWith(Map.of(
                "validRows", List.of(
                        Map.of("name", "Alice", "email", "alice@corp.com", "dept", "eng", "salary", "95000"),
                        Map.of("name", "Bob", "email", "bob@corp.com", "dept", "sales", "salary", "82000"),
                        Map.of("name", "Diana", "email", "diana@corp.com", "dept", "eng", "salary", "105000")),
                "headers", List.of("name", "email", "dept", "salary")));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
