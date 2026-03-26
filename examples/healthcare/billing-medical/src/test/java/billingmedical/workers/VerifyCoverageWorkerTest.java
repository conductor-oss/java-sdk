package billingmedical.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VerifyCoverageWorkerTest {
    private final VerifyCoverageWorker w = new VerifyCoverageWorker();
    @Test void taskDefName() { assertEquals("mbl_verify_coverage", w.getTaskDefName()); }
    @Test void verifies() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "cptCodes", List.of(Map.of("code", "99213")))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(80, r.getOutputData().get("coveragePercent"));
    }
}
