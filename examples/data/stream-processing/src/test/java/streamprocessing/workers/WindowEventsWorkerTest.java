package streamprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WindowEventsWorkerTest {

    private final WindowEventsWorker worker = new WindowEventsWorker();

    @Test void taskDefName() { assertEquals("st_window_events", worker.getTaskDefName()); }

    @Test void groupsEventsIntoWindows() {
        List<Map<String, Object>> events = List.of(
                Map.of("ts", 1000, "value", 10), Map.of("ts", 2000, "value", 12),
                Map.of("ts", 6000, "value", 50), Map.of("ts", 7000, "value", 55));
        TaskResult r = worker.execute(taskWith(Map.of("events", events, "windowSizeMs", 5000)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("windowCount"));
    }

    @Test void singleWindow() {
        List<Map<String, Object>> events = List.of(Map.of("ts", 1000, "value", 10), Map.of("ts", 2000, "value", 20));
        TaskResult r = worker.execute(taskWith(Map.of("events", events, "windowSizeMs", 5000)));
        assertEquals(1, r.getOutputData().get("windowCount"));
    }

    @Test void emptyEvents() {
        TaskResult r = worker.execute(taskWith(Map.of("events", List.of(), "windowSizeMs", 5000)));
        assertEquals(0, r.getOutputData().get("windowCount"));
    }

    @Test void nullEvents() {
        Map<String, Object> in = new HashMap<>(); in.put("events", null); in.put("windowSizeMs", 5000);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(0, r.getOutputData().get("windowCount"));
    }

    @Test void defaultWindowSize() {
        List<Map<String, Object>> events = List.of(Map.of("ts", 1000, "value", 10));
        TaskResult r = worker.execute(taskWith(Map.of("events", events)));
        assertEquals(1, r.getOutputData().get("windowCount"));
    }

    @Test void windowContainsEvents() {
        List<Map<String, Object>> events = List.of(Map.of("ts", 1000, "value", 10));
        TaskResult r = worker.execute(taskWith(Map.of("events", events, "windowSizeMs", 5000)));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> windows = (List<Map<String, Object>>) r.getOutputData().get("windows");
        assertEquals(1, windows.size());
        assertEquals(0L, ((Number) windows.get(0).get("windowStart")).longValue());
        assertEquals(5000L, ((Number) windows.get(0).get("windowEnd")).longValue());
        assertEquals(1, ((Number) windows.get(0).get("count")).intValue());
    }

    @Test void threeWindows() {
        List<Map<String, Object>> events = List.of(
                Map.of("ts", 1000, "value", 10), Map.of("ts", 6000, "value", 20), Map.of("ts", 11000, "value", 30));
        TaskResult r = worker.execute(taskWith(Map.of("events", events, "windowSizeMs", 5000)));
        assertEquals(3, r.getOutputData().get("windowCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
