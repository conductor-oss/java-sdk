package referralmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TrackReferralWorkerTest {
    private final TrackReferralWorker w = new TrackReferralWorker();
    @Test void taskDefName() { assertEquals("ref_track", w.getTaskDefName()); }
    @Test void tracks() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("referralId", "R1", "appointmentId", "APT-1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("completed", r.getOutputData().get("outcome"));
    }
}
