package clinicaldecision.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplyGuidelinesWorkerTest {
    private final ApplyGuidelinesWorker w = new ApplyGuidelinesWorker();
    @Test void taskDefName() { assertEquals("cds_apply_guidelines", w.getTaskDefName()); }
    @Test void applies() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("condition", "cardiovascular_risk", "clinicalData", Map.of("age", 62))));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("guidelineResults"));
    }
}
