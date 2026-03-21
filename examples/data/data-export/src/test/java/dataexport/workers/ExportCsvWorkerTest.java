package dataexport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExportCsvWorkerTest {

    private final ExportCsvWorker worker = new ExportCsvWorker();

    @Test
    void taskDefName() {
        assertEquals("dx_export_csv", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of(
                "data", List.of(Map.of("id", 1, "name", "Widget")),
                "headers", List.of("id", "name")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsFileInfo() {
        Task task = taskWith(Map.of(
                "data", List.of(Map.of("id", 1, "name", "Widget")),
                "headers", List.of("id", "name")));
        TaskResult result = worker.execute(task);
        assertEquals("export/data.csv", result.getOutputData().get("file"));
        assertNotNull(result.getOutputData().get("fileSize"));
        assertEquals(1, result.getOutputData().get("rowCount"));
    }

    @Test
    void handlesEmptyData() {
        Task task = taskWith(Map.of("data", List.of(), "headers", List.of("id")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("rowCount"));
    }

    @Test
    void handlesMultipleRows() {
        Task task = taskWith(Map.of(
                "data", List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3)),
                "headers", List.of("id")));
        TaskResult result = worker.execute(task);
        assertEquals(3, result.getOutputData().get("rowCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
