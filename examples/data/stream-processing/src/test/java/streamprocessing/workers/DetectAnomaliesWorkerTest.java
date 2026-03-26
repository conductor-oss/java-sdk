package streamprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectAnomaliesWorkerTest {

    private final DetectAnomaliesWorker worker = new DetectAnomaliesWorker();

    @Test void taskDefName() { assertEquals("st_detect_anomalies", worker.getTaskDefName()); }

    @Test void detectsAnomalousWindow() {
        List<Map<String, Object>> aggs = List.of(
                Map.of("windowStart", 0L, "avg", 10.0, "count", 3),
                Map.of("windowStart", 5000L, "avg", 51.0, "count", 3),
                Map.of("windowStart", 10000L, "avg", 10.67, "count", 3));
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", aggs)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        int anomalyCount = ((Number) r.getOutputData().get("anomalyCount")).intValue();
        assertTrue(anomalyCount > 0);
    }

    @Test void noAnomaliesWhenSimilar() {
        List<Map<String, Object>> aggs = List.of(
                Map.of("windowStart", 0L, "avg", 10.0), Map.of("windowStart", 5000L, "avg", 11.0));
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", aggs)));
        assertEquals(0, ((Number) r.getOutputData().get("anomalyCount")).intValue());
    }

    @Test void emptyAggregates() {
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", List.of())));
        assertEquals(0, ((Number) r.getOutputData().get("anomalyCount")).intValue());
    }

    @Test void nullAggregates() {
        Map<String, Object> in = new HashMap<>(); in.put("aggregates", null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(0, ((Number) r.getOutputData().get("anomalyCount")).intValue());
    }

    @Test void outputContainsGlobalAvg() {
        List<Map<String, Object>> aggs = List.of(Map.of("windowStart", 0L, "avg", 20.0));
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", aggs)));
        assertNotNull(r.getOutputData().get("globalAvg"));
    }

    @Test void singleWindowNoAnomaly() {
        List<Map<String, Object>> aggs = List.of(Map.of("windowStart", 0L, "avg", 10.0));
        TaskResult r = worker.execute(taskWith(Map.of("aggregates", aggs)));
        assertEquals(0, ((Number) r.getOutputData().get("anomalyCount")).intValue());
    }

    @Test void handlesMissingInput() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
