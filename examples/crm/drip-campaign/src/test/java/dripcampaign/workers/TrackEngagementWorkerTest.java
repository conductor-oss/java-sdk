package dripcampaign.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TrackEngagementWorkerTest {
    private final TrackEngagementWorker worker = new TrackEngagementWorker();
    @Test void taskDefName() { assertEquals("drp_track_engagement", worker.getTaskDefName()); }
    @Test void tracksEngagement() {
        TaskResult r = worker.execute(taskWith(Map.of("enrollmentId", "ENR-1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(78, r.getOutputData().get("engagementScore"));
        assertEquals(4, r.getOutputData().get("opens"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
