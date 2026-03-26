package capacitymgmttelecom.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MonitorWorkerTest {

    @Test
    void testMonitorWorker() {
        MonitorWorker worker = new MonitorWorker();
        assertEquals("cmt_monitor", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("region", "METRO-NE-3", "networkType", "5G"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(78, result.getOutputData().get("utilization"));
    }
}
