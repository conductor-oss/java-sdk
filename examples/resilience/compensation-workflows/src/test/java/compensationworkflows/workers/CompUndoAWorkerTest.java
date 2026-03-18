package compensationworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompUndoAWorkerTest {

    @Test
    void taskDefName() {
        CompUndoAWorker worker = new CompUndoAWorker();
        assertEquals("comp_undo_a", worker.getTaskDefName());
    }

    @Test
    void deletesFileCreatedByStepA() throws Exception {
        // Create a temp file mimicking StepA
        Path tempFile = Files.createTempFile("comp-resource-A-", ".txt");
        Files.writeString(tempFile, "test resource");
        assertTrue(Files.exists(tempFile));

        CompUndoAWorker worker = new CompUndoAWorker();
        Task task = taskWith(Map.of("original", tempFile.toString()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
        assertEquals(true, result.getOutputData().get("deleted"));
        assertFalse(Files.exists(tempFile), "File should be deleted");
    }

    @Test
    void completesEvenWithNonExistentFile() {
        CompUndoAWorker worker = new CompUndoAWorker();
        Task task = taskWith(Map.of("original", "resource-A-created"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
    }

    @Test
    void worksWithEmptyInput() {
        CompUndoAWorker worker = new CompUndoAWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
    }

    @Test
    void worksWithNullOriginal() {
        CompUndoAWorker worker = new CompUndoAWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("original", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
