package populationhealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StratifyRiskWorkerTest {
    private final StratifyRiskWorker w = new StratifyRiskWorker();
    @Test void taskDefName() { assertEquals("pop_stratify_risk", w.getTaskDefName()); }
    @Test void stratifies() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("cohortId", "C1", "populationData", Map.of("totalPatients", 2450))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("riskStrata"));
    }
}
