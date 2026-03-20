package thresholdalerting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CheckMetricWorkerTest {
    @Test void taskDefName() { assertEquals("th_check_metric", new CheckMetricWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("metricName","error_rate", "currentValue",12.5, "warningThreshold",5, "criticalThreshold",10)));
        TaskResult r = new CheckMetricWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("critical", r.getOutputData().get("severity"));
    }
}
