package metricscollection.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AggregateWorkerTest {
    @Test void taskDefName() { assertEquals("mc_aggregate", new AggregateWorker().getTaskDefName()); }
    @Test void aggregatesMetrics() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("appMetrics",45,"infraMetrics",32,"bizMetrics",18)));
        TaskResult r = new AggregateWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(95, r.getOutputData().get("totalMetrics"));
    }
}
