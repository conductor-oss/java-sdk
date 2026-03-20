package canaryrelease.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MonitorCanaryWorkerTest {
    private final MonitorCanaryWorker worker = new MonitorCanaryWorker();

    @Test void taskDefName() { assertEquals("cy_monitor_canary", worker.getTaskDefName()); }

    @Test void monitorsSuccessfully() {
        Task task = taskWith(Map.of("trafficPercent", 5, "monitorDuration", "5m"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("healthy"));
        assertEquals(0, r.getOutputData().get("anomaliesDetected"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
