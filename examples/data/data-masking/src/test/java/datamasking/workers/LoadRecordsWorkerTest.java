package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadRecordsWorkerTest {

    private final LoadRecordsWorker worker = new LoadRecordsWorker();

    @Test
    void taskDefName() {
        assertEquals("mk_load_records", worker.getTaskDefName());
    }

    @Test
    void loadsRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "ssn", "123-45-6789"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("count"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingRecords() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
