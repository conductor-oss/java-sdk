package predictivemonitoring.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PdmAlertWorkerTest {
    @Test void taskDefName() { assertEquals("pdm_alert", new PdmAlertWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("metricName","cpu", "predictedPeak",88.5, "breachLikelihood",72.3)));
        TaskResult r = new PdmAlertWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("alertSent"));
    }
}
