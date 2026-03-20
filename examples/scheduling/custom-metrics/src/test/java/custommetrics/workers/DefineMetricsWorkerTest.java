package custommetrics.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefineMetricsWorkerTest {
    @Test void taskDefName() { assertEquals("cus_define_metrics", new DefineMetricsWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("metricDefinitions","[m1,m2]")));
        TaskResult r = new DefineMetricsWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(4, r.getOutputData().get("registeredMetrics"));
    }
}
