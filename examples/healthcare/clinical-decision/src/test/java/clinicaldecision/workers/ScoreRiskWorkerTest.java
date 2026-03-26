package clinicaldecision.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScoreRiskWorkerTest {
    private final ScoreRiskWorker w = new ScoreRiskWorker();
    @Test void taskDefName() { assertEquals("cds_score_risk", w.getTaskDefName()); }
    @Test void scoresRisk() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("patientId", "P1", "clinicalData", Map.of(), "guidelineResults", Map.of())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("high", r.getOutputData().get("riskCategory"));
    }
}
