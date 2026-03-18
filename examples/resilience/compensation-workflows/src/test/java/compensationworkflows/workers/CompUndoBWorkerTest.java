package compensationworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompUndoBWorkerTest {

    @BeforeEach
    void setUp() {
        CompStepBWorker.clearRecords();
    }

    @Test
    void taskDefName() {
        CompUndoBWorker worker = new CompUndoBWorker();
        assertEquals("comp_undo_b", worker.getTaskDefName());
    }

    @Test
    void removesRecordFromStore() {
        // Insert a record first
        String recordKey = "record-B-test";
        CompStepBWorker.RECORDS.put(recordKey, "test value");

        CompUndoBWorker worker = new CompUndoBWorker();
        Task task = taskWith(Map.of("original", recordKey));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
        assertEquals(true, result.getOutputData().get("removed"));
        assertFalse(CompStepBWorker.RECORDS.containsKey(recordKey),
                "Record should be removed from store");
    }

    @Test
    void completesWhenRecordNotFound() {
        CompUndoBWorker worker = new CompUndoBWorker();
        Task task = taskWith(Map.of("original", "nonexistent-key"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
        assertEquals(false, result.getOutputData().get("removed"));
    }

    @Test
    void worksWithEmptyInput() {
        CompUndoBWorker worker = new CompUndoBWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
    }

    @Test
    void worksWithNullOriginal() {
        CompUndoBWorker worker = new CompUndoBWorker();
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
