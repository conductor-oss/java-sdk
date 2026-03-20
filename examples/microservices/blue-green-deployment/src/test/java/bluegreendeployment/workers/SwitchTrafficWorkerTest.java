package bluegreendeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SwitchTrafficWorkerTest {

    private final SwitchTrafficWorker worker = new SwitchTrafficWorker();

    @Test void taskDefName() { assertEquals("bg_switch_traffic", worker.getTaskDefName()); }

    @Test void switchesTraffic() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "from", "blue", "to", "green")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("switched"));
    }

    @Test void previousActiveIsFrom() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "from", "blue", "to", "green")));
        assertEquals("blue", r.getOutputData().get("previousActive"));
    }

    @Test void currentActiveIsTo() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "api-gw", "from", "blue", "to", "green")));
        assertEquals("green", r.getOutputData().get("currentActive"));
    }

    @Test void handlesNullFrom() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", "svc"); in.put("from", null); in.put("to", "green");
        TaskResult r = worker.execute(taskWith(in));
        assertEquals("blue", r.getOutputData().get("previousActive"));
    }

    @Test void handlesNullTo() {
        Map<String, Object> in = new HashMap<>(); in.put("serviceName", "svc"); in.put("from", "blue"); in.put("to", null);
        TaskResult r = worker.execute(taskWith(in));
        assertEquals("green", r.getOutputData().get("currentActive"));
    }

    @Test void handlesMissingInputs() {
        TaskResult r = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("switched"));
    }

    @Test void handlesReverseSwitch() {
        TaskResult r = worker.execute(taskWith(Map.of("serviceName", "svc", "from", "green", "to", "blue")));
        assertEquals("green", r.getOutputData().get("previousActive"));
        assertEquals("blue", r.getOutputData().get("currentActive"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
