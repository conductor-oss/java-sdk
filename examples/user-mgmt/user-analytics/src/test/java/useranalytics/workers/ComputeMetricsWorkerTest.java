package useranalytics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ComputeMetricsWorkerTest {
    private final ComputeMetricsWorker w = new ComputeMetricsWorker();
    @Test void taskDefName() { assertEquals("uan_compute_metrics", w.getTaskDefName()); }
    @SuppressWarnings("unchecked")
    @Test void computesMetrics() {
        TaskResult r = w.execute(t(Map.of("aggregated", Map.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        Map<String, Object> metrics = (Map<String, Object>) r.getOutputData().get("metrics");
        assertEquals(8450, metrics.get("dau"));
        assertEquals("68.5%", metrics.get("retention"));
    }
    @SuppressWarnings("unchecked")
    @Test void includesChurnRate() {
        TaskResult r = w.execute(t(Map.of("aggregated", Map.of())));
        Map<String, Object> metrics = (Map<String, Object>) r.getOutputData().get("metrics");
        assertEquals("3.2%", metrics.get("churnRate"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
