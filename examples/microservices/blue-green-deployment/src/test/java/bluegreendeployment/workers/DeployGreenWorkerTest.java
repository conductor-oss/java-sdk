package bluegreendeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeployGreenWorkerTest {

    private final DeployGreenWorker worker = new DeployGreenWorker();

    @Test void taskDefName() { assertEquals("bg_deploy_green", worker.getTaskDefName()); }

    @Test void deploysSuccessfully() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "imageTag", "api-gw:3.2.0")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("deployed"));
    }

    @Test void environmentIsGreen() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "imageTag", "api-gw:3.2.0")));
        assertEquals("green", r.getOutputData().get("environment"));
    }

    @Test void outputContainsImageTag() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "svc", "imageTag", "svc:1.0")));
        assertEquals("svc:1.0", r.getOutputData().get("imageTag"));
    }

    @Test void outputContainsServiceName() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "payment", "imageTag", "pay:2.0")));
        assertEquals("payment", r.getOutputData().get("serviceName"));
    }

    @Test void handlesNullServiceName() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", null); in.put("imageTag", "x:1");
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("unknown-service", r.getOutputData().get("serviceName"));
    }

    @Test void handlesNullImageTag() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", "svc"); in.put("imageTag", null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals("unknown:latest", r.getOutputData().get("imageTag"));
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("deployed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
