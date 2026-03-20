package leadnurturing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TrackWorkerTest {
    private final TrackWorker worker = new TrackWorker();

    @Test void taskDefName() { assertEquals("nur_track", worker.getTaskDefName()); }

    @Test void tracksEngagement() {
        TaskResult r = worker.execute(taskWith(Map.of("leadId", "L1", "deliveryId", "DLV-1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("engagement"));
    }

    @SuppressWarnings("unchecked")
    @Test void engagementHasExpectedFields() {
        TaskResult r = worker.execute(taskWith(Map.of("deliveryId", "DLV-1")));
        Map<String, Object> eng = (Map<String, Object>) r.getOutputData().get("engagement");
        assertTrue(eng.containsKey("opened"));
        assertTrue(eng.containsKey("clicked"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
