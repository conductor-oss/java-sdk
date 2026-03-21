package eventreplay.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("ep_generate_report", worker.getTaskDefName());
    }

    @Test
    void generatesReportForTwoSuccessfulReplays() {
        Task task = taskWith(Map.of(
                "replayResults", twoSuccessResults(),
                "totalReplayed", 2,
                "sourceStream", "order-events"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("rpt-fixed-001", result.getOutputData().get("reportId"));
        assertEquals(100, result.getOutputData().get("successRate"));
        assertEquals(2, result.getOutputData().get("successCount"));
        assertEquals(0, result.getOutputData().get("failCount"));
    }

    @Test
    void returnsFixedGeneratedAt() {
        Task task = taskWith(Map.of(
                "replayResults", twoSuccessResults(),
                "totalReplayed", 2,
                "sourceStream", "order-events"));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("generatedAt"));
    }

    @Test
    void returnsFixedReportId() {
        Task task = taskWith(Map.of(
                "replayResults", twoSuccessResults(),
                "totalReplayed", 2,
                "sourceStream", "order-events"));
        TaskResult result = worker.execute(task);

        assertEquals("rpt-fixed-001", result.getOutputData().get("reportId"));
    }

    @Test
    void handlesEmptyReplayResults() {
        Task task = taskWith(Map.of(
                "replayResults", List.of(),
                "totalReplayed", 0,
                "sourceStream", "order-events"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("successRate"));
        assertEquals(0, result.getOutputData().get("successCount"));
        assertEquals(0, result.getOutputData().get("failCount"));
    }

    @Test
    void handlesNullReplayResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("replayResults", null);
        input.put("totalReplayed", 0);
        input.put("sourceStream", "order-events");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("successCount"));
    }

    @Test
    void handlesNullSourceStream() {
        Map<String, Object> input = new HashMap<>();
        input.put("replayResults", twoSuccessResults());
        input.put("totalReplayed", 2);
        input.put("sourceStream", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("rpt-fixed-001", result.getOutputData().get("reportId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("successRate"));
        assertEquals("rpt-fixed-001", result.getOutputData().get("reportId"));
    }

    @Test
    void calculatesSingleSuccessRate() {
        List<Map<String, Object>> singleResult = List.of(
                Map.of("eventId", "evt-001", "replayStatus", "success",
                        "replayedAt", "2026-01-15T10:00:00Z"));
        Task task = taskWith(Map.of(
                "replayResults", singleResult,
                "totalReplayed", 1,
                "sourceStream", "order-events"));
        TaskResult result = worker.execute(task);

        assertEquals(100, result.getOutputData().get("successRate"));
        assertEquals(1, result.getOutputData().get("successCount"));
        assertEquals(0, result.getOutputData().get("failCount"));
    }

    private List<Map<String, Object>> twoSuccessResults() {
        return List.of(
                Map.of("eventId", "evt-001", "originalType", "order.created",
                        "replayStatus", "success", "replayedAt", "2026-01-15T10:00:00Z"),
                Map.of("eventId", "evt-005", "originalType", "order.created",
                        "replayStatus", "success", "replayedAt", "2026-01-15T10:00:00Z")
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
