package realtimeanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateAggregatesWorkerTest {

    private final UpdateAggregatesWorker worker = new UpdateAggregatesWorker();

    @Test
    void taskDefName() {
        assertEquals("ry_update_aggregates", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsAggregates() {
        Task task = taskWith(Map.of("windowMetrics", Map.of("eventCount", 8)));
        TaskResult result = worker.execute(task);

        Map<String, Object> agg = (Map<String, Object>) result.getOutputData().get("aggregates");
        assertNotNull(agg);
        assertEquals(4520, agg.get("totalEventsLast1h"));
        assertEquals("820ms", agg.get("p99Latency"));
    }

    @Test
    void returnsUpdatedCount() {
        Task task = taskWith(Map.of("windowMetrics", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(7, result.getOutputData().get("updatedCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void includesCurrentWindow() {
        Map<String, Object> metrics = Map.of("eventCount", 10, "avgLatency", "200ms");
        Task task = taskWith(Map.of("windowMetrics", metrics));
        TaskResult result = worker.execute(task);

        Map<String, Object> agg = (Map<String, Object>) result.getOutputData().get("aggregates");
        assertEquals(metrics, agg.get("currentWindow"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
