package streamprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestStreamWorkerTest {

    private final IngestStreamWorker worker = new IngestStreamWorker();

    @Test void taskDefName() { assertEquals("st_ingest_stream", worker.getTaskDefName()); }

    @Test void ingestsEvents() {
        Task task = taskWith(Map.of("events", List.of(Map.of("ts", 1000, "value", 10), Map.of("ts", 2000, "value", 20))));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("eventCount"));
        assertNotNull(r.getOutputData().get("events"));
    }

    @Test void handlesEmptyEvents() {
        Task task = taskWith(Map.of("events", List.of()));
        TaskResult r = worker.execute(task);
        assertEquals(0, r.getOutputData().get("eventCount"));
    }

    @Test void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>(); input.put("events", null);
        TaskResult r = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(0, r.getOutputData().get("eventCount"));
    }

    @Test void handlesMissingEvents() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(0, r.getOutputData().get("eventCount"));
    }

    @Test void preservesEventData() {
        List<Map<String, Object>> events = List.of(Map.of("ts", 5000, "value", 42, "source", "s1"));
        TaskResult r = worker.execute(taskWith(Map.of("events", events)));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> out = (List<Map<String, Object>>) r.getOutputData().get("events");
        assertEquals(1, out.size());
        assertEquals(42, ((Number) out.get(0).get("value")).intValue());
    }

    @Test void handlesLargeEventList() {
        List<Map<String, Object>> events = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) events.add(Map.of("ts", i * 1000, "value", i));
        TaskResult r = worker.execute(taskWith(Map.of("events", events)));
        assertEquals(100, r.getOutputData().get("eventCount"));
    }

    @Test void singleEvent() {
        TaskResult r = worker.execute(taskWith(Map.of("events", List.of(Map.of("ts", 0, "value", 1)))));
        assertEquals(1, r.getOutputData().get("eventCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
