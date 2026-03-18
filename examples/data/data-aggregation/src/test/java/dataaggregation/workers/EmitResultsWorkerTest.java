package dataaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitResultsWorkerTest {

    private final EmitResultsWorker worker = new EmitResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("agg_emit_results", worker.getTaskDefName());
    }

    @Test
    void emitsSummaryWithGroupCount() {
        Task task = taskWith(Map.of(
                "report", List.of("east: count=2, sum=300.0, avg=150.0"),
                "groupCount", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Aggregation complete: 1 groups reported", result.getOutputData().get("summary"));
    }

    @Test
    void emitsSummaryWithMultipleGroups() {
        Task task = taskWith(Map.of(
                "report", List.of("east: count=2", "west: count=1", "north: count=3"),
                "groupCount", 3));
        TaskResult result = worker.execute(task);

        assertEquals("Aggregation complete: 3 groups reported", result.getOutputData().get("summary"));
    }

    @Test
    void handlesZeroGroups() {
        Task task = taskWith(Map.of(
                "report", List.of(),
                "groupCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Aggregation complete: 0 groups reported", result.getOutputData().get("summary"));
    }

    @Test
    void handlesNullReport() {
        Map<String, Object> input = new HashMap<>();
        input.put("report", null);
        input.put("groupCount", 2);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Aggregation complete: 2 groups reported", result.getOutputData().get("summary"));
    }

    @Test
    void handlesNullGroupCount() {
        Map<String, Object> input = new HashMap<>();
        input.put("report", List.of("line1"));
        input.put("groupCount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Aggregation complete: 0 groups reported", result.getOutputData().get("summary"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Aggregation complete: 0 groups reported", result.getOutputData().get("summary"));
    }

    @Test
    void summaryContainsCorrectFormat() {
        Task task = taskWith(Map.of(
                "report", List.of("g1: count=5"),
                "groupCount", 5));
        TaskResult result = worker.execute(task);

        String summary = (String) result.getOutputData().get("summary");
        assertTrue(summary.startsWith("Aggregation complete:"));
        assertTrue(summary.endsWith("groups reported"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
