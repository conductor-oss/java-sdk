package streamprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateWindowsWorkerTest {

    private final AggregateWindowsWorker worker = new AggregateWindowsWorker();

    @Test void taskDefName() { assertEquals("st_aggregate_windows", worker.getTaskDefName()); }

    @Test void aggregatesSingleWindow() {
        Map<String, Object> window = Map.of("windowStart", 0L, "events",
                List.of(Map.of("value", 10), Map.of("value", 20), Map.of("value", 30)), "count", 3);
        TaskResult r = worker.execute(taskWith(Map.of("windows", List.of(window))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggs = (List<Map<String, Object>>) r.getOutputData().get("aggregates");
        assertEquals(1, aggs.size());
        assertEquals(60.0, ((Number) aggs.get(0).get("sum")).doubleValue());
        assertEquals(20.0, ((Number) aggs.get(0).get("avg")).doubleValue());
        assertEquals(30.0, ((Number) aggs.get(0).get("max")).doubleValue());
    }

    @Test void aggregatesMultipleWindows() {
        Map<String, Object> w1 = Map.of("windowStart", 0L, "events", List.of(Map.of("value", 10)), "count", 1);
        Map<String, Object> w2 = Map.of("windowStart", 5000L, "events", List.of(Map.of("value", 50)), "count", 1);
        TaskResult r = worker.execute(taskWith(Map.of("windows", List.of(w1, w2))));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggs = (List<Map<String, Object>>) r.getOutputData().get("aggregates");
        assertEquals(2, aggs.size());
    }

    @Test void handlesEmptyWindows() {
        TaskResult r = worker.execute(taskWith(Map.of("windows", List.of())));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggs = (List<Map<String, Object>>) r.getOutputData().get("aggregates");
        assertEquals(0, aggs.size());
    }

    @Test void handlesNullWindows() {
        Map<String, Object> in = new HashMap<>(); in.put("windows", null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesZeroValues() {
        Map<String, Object> w = Map.of("windowStart", 0L, "events", List.of(Map.of("value", 0), Map.of("value", 0)), "count", 2);
        TaskResult r = worker.execute(taskWith(Map.of("windows", List.of(w))));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggs = (List<Map<String, Object>>) r.getOutputData().get("aggregates");
        assertEquals(0.0, ((Number) aggs.get(0).get("sum")).doubleValue());
        assertEquals(0.0, ((Number) aggs.get(0).get("avg")).doubleValue());
    }

    @Test void computesMaxCorrectly() {
        Map<String, Object> w = Map.of("windowStart", 0L, "events",
                List.of(Map.of("value", 5), Map.of("value", 100), Map.of("value", 50)), "count", 3);
        TaskResult r = worker.execute(taskWith(Map.of("windows", List.of(w))));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggs = (List<Map<String, Object>>) r.getOutputData().get("aggregates");
        assertEquals(100.0, ((Number) aggs.get(0).get("max")).doubleValue());
    }

    @Test void handlesMissingInput() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
