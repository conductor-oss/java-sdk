package clickstreamanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestClicksWorkerTest {

    private final IngestClicksWorker worker = new IngestClicksWorker();

    @Test
    void taskDefName() {
        assertEquals("ck_ingest_clicks", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void ingestsTenEvents() {
        Task task = taskWith(Map.of("clickData", Map.of("source", "web-tracker")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10, result.getOutputData().get("clickCount"));
        List<Map<String, Object>> events = (List<Map<String, Object>>) result.getOutputData().get("events");
        assertEquals(10, events.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void eventsHaveRequiredFields() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events = (List<Map<String, Object>>) result.getOutputData().get("events");
        Map<String, Object> first = events.get(0);
        assertNotNull(first.get("userId"));
        assertNotNull(first.get("page"));
        assertNotNull(first.get("ts"));
        assertNotNull(first.get("action"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
