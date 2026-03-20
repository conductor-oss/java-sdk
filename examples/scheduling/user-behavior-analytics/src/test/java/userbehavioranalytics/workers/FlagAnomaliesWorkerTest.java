package userbehavioranalytics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FlagAnomaliesWorkerTest {
    @Test void taskDefName() { assertEquals("uba_flag_anomalies", new FlagAnomaliesWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("userId","user-1", "riskScore",78, "riskThreshold",70)));
        TaskResult r = new FlagAnomaliesWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("flagged"));
    }
}
