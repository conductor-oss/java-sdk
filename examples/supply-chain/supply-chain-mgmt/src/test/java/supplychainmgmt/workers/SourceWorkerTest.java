package supplychainmgmt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SourceWorkerTest {

    private final SourceWorker worker = new SourceWorker();

    @Test
    void taskDefName() {
        assertEquals("scm_source", worker.getTaskDefName());
    }

    @Test
    void sourcesMaterials() {
        List<Map<String, Object>> materials = List.of(
                Map.of("name", "steel", "qty", 200));
        Task task = taskWith(Map.of("materials", materials));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("sourcedMaterials"));
        assertEquals(3, result.getOutputData().get("suppliersUsed"));
    }

    @Test
    void handlesNullMaterials() {
        Map<String, Object> input = new HashMap<>();
        input.put("materials", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyMaterials() {
        Task task = taskWith(Map.of("materials", List.of()));
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
