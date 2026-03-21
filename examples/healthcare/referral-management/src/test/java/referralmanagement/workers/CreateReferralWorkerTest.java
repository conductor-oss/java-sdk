package referralmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateReferralWorkerTest {
    private final CreateReferralWorker w = new CreateReferralWorker();
    @Test void taskDefName() { assertEquals("ref_create", w.getTaskDefName()); }
    @Test void creates() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("referralId", "R1", "patientId", "P1", "specialty", "Ortho", "reason", "pain")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("insurancePlan"));
    }
}
