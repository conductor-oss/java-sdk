package coldchain.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MonitorTempWorkerTest {
    @Test void taskDefName() { assertEquals("cch_monitor_temp", new MonitorTempWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("shipmentId","COLD-001","product","Pharma","minTemp",2,"maxTemp",8,"currentTemp",3.2,"status","ok")));
        TaskResult r = new MonitorTempWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
