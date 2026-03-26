package industrialiot.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PredictiveAnalysisWorkerTest {
    private final PredictiveAnalysisWorker worker = new PredictiveAnalysisWorker();
    @Test void taskDefName() { assertEquals("iit_predictive_analysis", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("failureProbability"));
        assertEquals("bearing_assembly", r.getOutputData().get("predictedComponent"));
        assertEquals("45 days", r.getOutputData().get("estimatedRemainingLife"));
    }
}
