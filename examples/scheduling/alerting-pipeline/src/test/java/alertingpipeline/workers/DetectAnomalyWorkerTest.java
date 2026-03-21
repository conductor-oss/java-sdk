package alertingpipeline.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DetectAnomalyWorkerTest {
    @Test void taskDefName() { assertEquals("alt_detect_anomaly", new DetectAnomalyWorker().getTaskDefName()); }
    @Test void detectsAnomaly() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("metricName","cpu","currentValue",95,"threshold",80)));
        TaskResult r = new DetectAnomalyWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("isAnomaly"));
        assertEquals("critical", r.getOutputData().get("severity"));
    }
    @Test void noAnomalyBelowThreshold() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("metricName","cpu","currentValue",50,"threshold",80)));
        TaskResult r = new DetectAnomalyWorker().execute(t);
        assertEquals(false, r.getOutputData().get("isAnomaly"));
        assertEquals("info", r.getOutputData().get("severity"));
    }
}
