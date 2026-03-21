package investmentworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class MonitorWorkerTest {
    private final MonitorWorker worker = new MonitorWorker();
    @Test void taskDefName() { assertEquals("ivt_monitor", worker.getTaskDefName()); }
    @Test void setsUpMonitoring() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("tradeId", "TRD-1", "executedPrice", 185.25)));
        TaskResult r = worker.execute(t);
        assertEquals("active", r.getOutputData().get("monitoringStatus"));
        assertNotNull(r.getOutputData().get("alertsConfigured"));
    }
}
