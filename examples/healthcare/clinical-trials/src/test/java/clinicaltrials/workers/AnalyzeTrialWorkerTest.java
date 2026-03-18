package clinicaltrials.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeTrialWorkerTest {
    private final AnalyzeTrialWorker w = new AnalyzeTrialWorker();

    @Test void taskDefName() { assertEquals("clt_analyze", w.getTaskDefName()); }

    @Test void detectsImprovement() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("monitoringData", Map.of("biomarkerChange", -12.5, "compliance", 95, "adverseEvents", 0))));
        TaskResult r = w.execute(t);
        assertEquals("improvement", r.getOutputData().get("outcome"));
    }

    @Test void noChangeForSmallEffect() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("monitoringData", Map.of("biomarkerChange", 1.0, "compliance", 90, "adverseEvents", 0))));
        TaskResult r = w.execute(t);
        assertEquals("no_change", r.getOutputData().get("outcome"));
    }
}
