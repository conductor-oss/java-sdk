package datamasking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitMaskedWorkerTest {

    private final EmitMaskedWorker worker = new EmitMaskedWorker();

    @Test
    void taskDefName() {
        assertEquals("mk_emit_masked", worker.getTaskDefName());
    }

    @Test
    void returnsSummary() {
        Task task = taskWith(Map.of(
                "records", List.of(Map.of("name", "Alice", "ssn", "***-**-6789")),
                "fieldsDetected", 3,
                "recordCount", 1));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.contains("3 PII field types"));
        assertTrue(summary.contains("1 records"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("records", List.of(), "fieldsDetected", 0, "recordCount", 0));
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
