package datawarehouseload.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateMetadataWorkerTest {

    private final UpdateMetadataWorker worker = new UpdateMetadataWorker();

    @Test
    void taskDefName() {
        assertEquals("wh_update_metadata", worker.getTaskDefName());
    }

    @Test
    void generatesSummary() {
        Task task = taskWith(Map.of("targetTable", "fact_revenue", "recordsLoaded", 5, "validationPassed", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("5"));
        assertTrue(summary.contains("fact_revenue"));
        assertTrue(summary.contains("true"));
    }

    @Test
    void includesLastLoadTime() {
        Task task = taskWith(Map.of("targetTable", "fact_test", "recordsLoaded", 1, "validationPassed", true));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("lastLoadTime"));
    }

    @Test
    void handlesValidationFalse() {
        Task task = taskWith(Map.of("targetTable", "fact_test", "recordsLoaded", 3, "validationPassed", false));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("false"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String summary = (String) result.getOutputData().get("summary");
        assertNotNull(summary);
    }

    @Test
    void summaryContainsTableName() {
        Task task = taskWith(Map.of("targetTable", "dim_product", "recordsLoaded", 10, "validationPassed", true));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("dim_product"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
