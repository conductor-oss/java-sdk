package slascheduling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TrackComplianceWorkerTest {
    @Test void taskDefName() { assertEquals("sla_track_compliance", new TrackComplianceWorker().getTaskDefName()); }
    @Test void tracksCompliance() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("slaPolicy","premium")));
        TaskResult r = new TrackComplianceWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("100%", r.getOutputData().get("complianceRate"));
    }
}
