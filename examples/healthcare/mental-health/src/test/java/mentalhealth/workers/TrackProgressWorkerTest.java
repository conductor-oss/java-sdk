package mentalhealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrackProgressWorkerTest {
    private final TrackProgressWorker w = new TrackProgressWorker();
    @Test void taskDefName() { assertEquals("mh_track_progress", w.getTaskDefName()); }
    @Test void tracksProgress() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "treatmentPlan", Map.of("therapy", "CBT"), "baselineScore", 14)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("trackingActive"));
    }
}
