package capacitymonitoring.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CapAlertWorkerTest {
    @Test void taskDefName() { assertEquals("cap_alert", new CapAlertWorker().getTaskDefName()); }
    @Test void alertsWhenLow() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("cluster","prod","daysUntilDiskFull",22)));
        TaskResult r = new CapAlertWorker().execute(t);
        assertEquals(true, r.getOutputData().get("alertSent"));
    }
    @Test void noAlertWhenSafe() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("cluster","prod","daysUntilDiskFull",60)));
        TaskResult r = new CapAlertWorker().execute(t);
        assertEquals(false, r.getOutputData().get("alertSent"));
    }
}
