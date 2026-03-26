package bluegreendeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateGreenWorkerTest {

    private final ValidateGreenWorker worker = new ValidateGreenWorker();

    @Test void taskDefName() { assertEquals("bg_validate_green", worker.getTaskDefName()); }

    @Test void validatesSuccessfully() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "environment", "green")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("healthy"));
    }

    @Test void allTestsPass() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "environment", "green")));
        assertEquals(24, r.getOutputData().get("testsRun"));
        assertEquals(24, r.getOutputData().get("testsPassed"));
    }

    @Test void outputContainsEnvironment() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "svc", "environment", "green")));
        assertEquals("green", r.getOutputData().get("environment"));
    }

    @Test void handlesNullServiceName() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", null); in.put("environment", "green");
        TaskResult r = worker.execute(taskWith(in));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesNullEnvironment() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", "svc"); in.put("environment", null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals("green", r.getOutputData().get("environment"));
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("healthy"));
    }

    @Test void handlesCustomEnvironment() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "svc", "environment", "staging")));
        assertEquals("staging", r.getOutputData().get("environment"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
