package billingmedical.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SubmitClaimWorkerTest {
    private final SubmitClaimWorker w = new SubmitClaimWorker();
    @Test void taskDefName() { assertEquals("mbl_submit_claim", w.getTaskDefName()); }
    @Test void submits() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("encounterId", "E1", "patientId", "P1", "providerId", "PR1", "totalCharge", 250, "coverage", 80)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("claimId"));
    }
}
