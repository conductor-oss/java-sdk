package datamigration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformSchemaWorkerTest {

    private final TransformSchemaWorker worker = new TransformSchemaWorker();

    @Test
    void taskDefName() {
        assertEquals("mi_transform_schema", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void transformsIdFormat() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@old.com", "dept_id", 10, "hire_date", "2020-01-15"));
        Task task = taskWith(Map.of("validRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> transformed = (List<Map<String, Object>>) result.getOutputData().get("transformed");
        assertEquals(1, transformed.size());
        assertEquals("EMP-00001", transformed.get(0).get("employee_id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void transformsEmailDomain() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 2, "name", "Bob", "email", "bob@old.com", "dept_id", 20, "hire_date", "2019-06-01"));
        Task task = taskWith(Map.of("validRecords", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> transformed = (List<Map<String, Object>>) result.getOutputData().get("transformed");
        assertEquals("bob@new.com", transformed.get(0).get("email_address"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void transformsDepartmentCode() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@old.com", "dept_id", 10, "hire_date", "2020-01-15"));
        Task task = taskWith(Map.of("validRecords", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> transformed = (List<Map<String, Object>>) result.getOutputData().get("transformed");
        assertEquals("DEPT-10", transformed.get(0).get("department_code"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("validRecords", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("transformedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
