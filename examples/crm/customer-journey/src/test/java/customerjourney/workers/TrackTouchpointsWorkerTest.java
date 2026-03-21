package customerjourney.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrackTouchpointsWorkerTest {

    private final TrackTouchpointsWorker worker = new TrackTouchpointsWorker();

    @Test
    void taskDefName() {
        assertEquals("cjy_track_touchpoints", worker.getTaskDefName());
    }

    @Test
    void returnsTouchpoints() {
        Task task = taskWith(Map.of("customerId", "CUST-001", "timeWindow", "30d"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("touchpoints"));
        assertTrue(result.getOutputData().get("touchpoints") instanceof List);
    }

    @Test
    void returnsTouchpointCount() {
        Task task = taskWith(Map.of("customerId", "CUST-001", "timeWindow", "30d"));
        TaskResult result = worker.execute(task);

        assertEquals(14, result.getOutputData().get("touchpointCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void touchpointsHaveExpectedFields() {
        Task task = taskWith(Map.of("customerId", "CUST-001", "timeWindow", "30d"));
        TaskResult result = worker.execute(task);

        List<Map<String, String>> touchpoints = (List<Map<String, String>>) result.getOutputData().get("touchpoints");
        assertFalse(touchpoints.isEmpty());
        assertTrue(touchpoints.get(0).containsKey("channel"));
        assertTrue(touchpoints.get(0).containsKey("action"));
        assertTrue(touchpoints.get(0).containsKey("timestamp"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
