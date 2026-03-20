package datamigration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadTargetWorkerTest {

    private final LoadTargetWorker worker = new LoadTargetWorker();

    @Test
    void taskDefName() {
        assertEquals("mi_load_target", worker.getTaskDefName());
    }

    @Test
    void loadsRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("employee_id", "EMP-00001", "full_name", "Alice"),
                Map.of("employee_id", "EMP-00002", "full_name", "Bob"));
        Task task = taskWith(Map.of("transformedRecords", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("loadedCount"));
        assertEquals(0, result.getOutputData().get("failedCount"));
        assertEquals("employees_v2", result.getOutputData().get("targetTable"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("transformedRecords", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("loadedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
