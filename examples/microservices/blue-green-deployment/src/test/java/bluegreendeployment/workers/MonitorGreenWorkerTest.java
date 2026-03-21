package bluegreendeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonitorGreenWorkerTest {

    private final MonitorGreenWorker worker = new MonitorGreenWorker();

    @Test void taskDefName() { assertEquals("bg_monitor_green", worker.getTaskDefName()); }

    @Test void monitorsSuccessfully() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "version", "3.2.0")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("healthy"));
    }

    @Test void errorRateIsLow() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "version", "3.2.0")));
        assertEquals(0.01, r.getOutputData().get("errorRate"));
    }

    @Test void outputContainsVersion() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "svc", "version", "1.5.0")));
        assertEquals("1.5.0", r.getOutputData().get("version"));
    }

    @Test void handlesNullVersion() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", "svc"); in.put("version", null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals("unknown", r.getOutputData().get("version"));
    }

    @Test void handlesNullServiceName() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", null); in.put("version", "1.0");
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("healthy"));
    }

    @Test void handlesVariousVersions() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "svc", "version", "10.0.0-beta")));
        assertEquals("10.0.0-beta", r.getOutputData().get("version"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
