package calendarintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FetchEventsWorkerTest {
    private final FetchEventsWorker worker = new FetchEventsWorker();
    @Test void taskDefName() { assertEquals("cal_fetch_events", worker.getTaskDefName()); }
    @Test void fetchesEvents() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("calendarId", "test@example.com", "syncWindow", "2026-03-08/2026-03-15")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("eventCount"));
        assertNotNull(r.getOutputData().get("events"));
    }
}
