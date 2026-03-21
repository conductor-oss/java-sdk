package useranalytics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectEventsWorkerTest {
    private final CollectEventsWorker w = new CollectEventsWorker();
    @Test void taskDefName() { assertEquals("uan_collect_events", w.getTaskDefName()); }
    @Test void collects() { TaskResult r = w.execute(t(Map.of("dateRange", Map.of("start", "2024-01-01")))); assertEquals(TaskResult.Status.COMPLETED, r.getStatus()); assertEquals(284500, r.getOutputData().get("totalEvents")); }
    @Test void includesEvents() { TaskResult r = w.execute(t(Map.of("dateRange", Map.of()))); assertNotNull(r.getOutputData().get("events")); }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
