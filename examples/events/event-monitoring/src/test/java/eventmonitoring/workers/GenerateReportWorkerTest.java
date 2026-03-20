package eventmonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("em_generate_report", worker.getTaskDefName());
    }

    @Test
    void generatesReportWithNormalAlertLevel() {
        Task task = taskWith(Map.of(
                "pipeline", "order-events-pipeline",
                "throughput", Map.of("eventsPerSec", "51.4", "totalEvents", 15420),
                "latency", Map.of("avgMs", 45, "p95Ms", 120, "p99Ms", 500),
                "errorRate", Map.of("percentage", "2.08", "failed", 320, "total", 15420)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reportGenerated"));
        assertEquals("normal", result.getOutputData().get("alertLevel"));
    }

    @Test
    void returnsWarningForHighErrorRate() {
        Task task = taskWith(Map.of(
                "pipeline", "order-events-pipeline",
                "throughput", Map.of("eventsPerSec", "10.0"),
                "latency", Map.of("avgMs", 100),
                "errorRate", Map.of("percentage", "7.50", "failed", 750, "total", 10000)));
        TaskResult result = worker.execute(task);

        assertEquals("warning", result.getOutputData().get("alertLevel"));
    }

    @Test
    void returnsCriticalForVeryHighErrorRate() {
        Task task = taskWith(Map.of(
                "pipeline", "order-events-pipeline",
                "throughput", Map.of("eventsPerSec", "5.0"),
                "latency", Map.of("avgMs", 200),
                "errorRate", Map.of("percentage", "15.00", "failed", 1500, "total", 10000)));
        TaskResult result = worker.execute(task);

        assertEquals("critical", result.getOutputData().get("alertLevel"));
    }

    @Test
    void handlesNullPipeline() {
        Map<String, Object> input = new HashMap<>();
        input.put("pipeline", null);
        input.put("throughput", Map.of("eventsPerSec", "51.4"));
        input.put("latency", Map.of("avgMs", 45));
        input.put("errorRate", Map.of("percentage", "2.08"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reportGenerated"));
    }

    @Test
    void handlesNullErrorRate() {
        Map<String, Object> input = new HashMap<>();
        input.put("pipeline", "my-pipeline");
        input.put("throughput", Map.of("eventsPerSec", "51.4"));
        input.put("latency", Map.of("avgMs", 45));
        input.put("errorRate", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("normal", result.getOutputData().get("alertLevel"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reportGenerated"));
        assertEquals("normal", result.getOutputData().get("alertLevel"));
    }

    @Test
    void returnsNormalForBorderlineErrorRate() {
        Task task = taskWith(Map.of(
                "pipeline", "test-pipeline",
                "throughput", Map.of("eventsPerSec", "30.0"),
                "latency", Map.of("avgMs", 60),
                "errorRate", Map.of("percentage", "4.99", "failed", 499, "total", 10000)));
        TaskResult result = worker.execute(task);

        assertEquals("normal", result.getOutputData().get("alertLevel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
