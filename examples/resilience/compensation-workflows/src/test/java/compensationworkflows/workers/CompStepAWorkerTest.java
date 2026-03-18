package compensationworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompStepAWorkerTest {

    @Test
    void taskDefName() {
        CompStepAWorker worker = new CompStepAWorker();
        assertEquals("comp_step_a", worker.getTaskDefName());
    }

    @Test
    void createsRealTempFile() {
        CompStepAWorker worker = new CompStepAWorker();
        Task task = taskWith(Map.of("step", "A"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("resource-A-created", result.getOutputData().get("result"));

        // Verify a real file was created
        String resourcePath = (String) result.getOutputData().get("resourcePath");
        assertNotNull(resourcePath, "resourcePath should be set");
        assertTrue(Files.exists(Path.of(resourcePath)), "Temp file should exist");

        // Cleanup
        try { Files.deleteIfExists(Path.of(resourcePath)); } catch (Exception ignored) {}
    }

    @Test
    void outputContainsResultKey() {
        CompStepAWorker worker = new CompStepAWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("result"));

        // Cleanup
        String path = (String) result.getOutputData().get("resourcePath");
        if (path != null) {
            try { Files.deleteIfExists(Path.of(path)); } catch (Exception ignored) {}
        }
    }

    @Test
    void worksWithEmptyInput() {
        CompStepAWorker worker = new CompStepAWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("resource-A-created", result.getOutputData().get("result"));

        // Cleanup
        String path = (String) result.getOutputData().get("resourcePath");
        if (path != null) {
            try { Files.deleteIfExists(Path.of(path)); } catch (Exception ignored) {}
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
