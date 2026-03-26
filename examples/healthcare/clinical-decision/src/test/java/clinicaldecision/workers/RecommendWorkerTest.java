package clinicaldecision.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecommendWorkerTest {
    private final RecommendWorker w = new RecommendWorker();
    @Test void taskDefName() { assertEquals("cds_recommend", w.getTaskDefName()); }
    @Test void recommends() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "condition", "cvd", "riskScore", 22.5, "guidelineResults", Map.of())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("recommendations"));
    }
}
