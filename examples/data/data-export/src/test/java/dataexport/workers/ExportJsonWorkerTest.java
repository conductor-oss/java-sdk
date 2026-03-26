package dataexport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExportJsonWorkerTest {

    private final ExportJsonWorker worker = new ExportJsonWorker();

    @Test
    void taskDefName() {
        assertEquals("dx_export_json", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("data", List.of(Map.of("id", 1))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsFileInfo() {
        Task task = taskWith(Map.of("data", List.of(Map.of("id", 1))));
        TaskResult result = worker.execute(task);
        assertEquals("export/data.json", result.getOutputData().get("file"));
        assertNotNull(result.getOutputData().get("fileSize"));
        assertEquals(1, result.getOutputData().get("recordCount"));
    }

    @Test
    void handlesEmptyData() {
        Task task = taskWith(Map.of("data", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recordCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
