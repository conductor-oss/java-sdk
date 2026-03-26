package streamprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmitResultsWorkerTest {

    private final EmitResultsWorker worker = new EmitResultsWorker();

    @Test void taskDefName() { assertEquals("st_emit_results", worker.getTaskDefName()); }

    @Test void emitsSummary() {
        TaskResult r = worker.execute(taskWith(Map.of(
                "aggregates", List.of(Map.of("windowStart", 0L), Map.of("windowStart", 5000L)),
                "anomalies", List.of(Map.of("windowStart", 5000L)))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("Stream processed: 2 windows, 1 anomalies detected", r.getOutputData().get("summary"));
    }

    @Test void noAnomalies() {
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", List.of(Map.of("w", 1)), "anomalies", List.of())));
        assertEquals("Stream processed: 1 windows, 0 anomalies detected", r.getOutputData().get("summary"));
    }

    @Test void emptyInputs() {
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", List.of(), "anomalies", List.of())));
        assertEquals("Stream processed: 0 windows, 0 anomalies detected", r.getOutputData().get("summary"));
    }

    @Test void nullInputs() {
        Map<String, Object> in = new HashMap<>(); in.put("aggregates", null); in.put("anomalies", null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void multipleAnomalies() {
        TaskResult r = worker.execute(taskWith(Map.of(
                "aggregates", List.of(Map.of("w", 1), Map.of("w", 2), Map.of("w", 3)),
                "anomalies", List.of(Map.of("a", 1), Map.of("a", 2)))));
        String summary = (String) r.getOutputData().get("summary");
        assertTrue(summary.contains("3 windows"));
        assertTrue(summary.contains("2 anomalies"));
    }

    @Test void summaryFormat() {
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", List.of(Map.of("w", 1)), "anomalies", List.of())));
        String summary = (String) r.getOutputData().get("summary");
        assertTrue(summary.startsWith("Stream processed:"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
