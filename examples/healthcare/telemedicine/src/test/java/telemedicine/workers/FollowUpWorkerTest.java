package telemedicine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FollowUpWorkerTest {
    private final FollowUpWorker w = new FollowUpWorker();
    @Test void taskDefName() { assertEquals("tlm_followup", w.getTaskDefName()); }
    @Test void schedulesFollowUp() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("visitId", "V1", "patientId", "P1", "providerId", "D1", "followUpNeeded", true)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("followUpScheduled"));
        assertNotNull(r.getOutputData().get("followUpDate"));
    }
}
