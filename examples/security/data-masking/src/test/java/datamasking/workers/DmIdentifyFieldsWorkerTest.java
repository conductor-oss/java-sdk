package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DmIdentifyFieldsWorkerTest {

    private final DmIdentifyFieldsWorker worker = new DmIdentifyFieldsWorker();

    @Test
    void taskDefName() {
        assertEquals("dm_identify_fields", worker.getTaskDefName());
    }

    @Test
    void completesWithFieldsId() {
        Task task = taskWith(Map.of("dataSource", "customer-database", "purpose", "analytics"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("IDENTIFY_FIELDS-1392", result.getOutputData().get("identify_fieldsId"));
    }

    @Test
    void outputContainsSuccess() {
        Task task = taskWith(Map.of("dataSource", "user-db", "purpose", "reporting"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullDataSource() {
        Map<String, Object> input = new HashMap<>();
        input.put("dataSource", null);
        input.put("purpose", "analytics");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("identify_fieldsId"));
    }

    @Test
    void outputIsDeterministic() {
        Task task = taskWith(Map.of("dataSource", "db", "purpose", "test"));
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("identify_fieldsId"), r2.getOutputData().get("identify_fieldsId"));
    }

    @Test
    void outputHasTwoEntries() {
        Task task = taskWith(Map.of("dataSource", "db"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
