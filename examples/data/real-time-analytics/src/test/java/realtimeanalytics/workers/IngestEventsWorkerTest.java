package realtimeanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestEventsWorkerTest {

    private final IngestEventsWorker worker = new IngestEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("ry_ingest_events", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsEvents() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("events"));
    }

    @Test
    void returnsEventCount() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(8, result.getOutputData().get("eventCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void eventsHaveRequiredFields() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> events = (List<Map<String, Object>>) result.getOutputData().get("events");
        for (Map<String, Object> e : events) {
            assertNotNull(e.get("eventId"));
            assertNotNull(e.get("type"));
            assertNotNull(e.get("userId"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
