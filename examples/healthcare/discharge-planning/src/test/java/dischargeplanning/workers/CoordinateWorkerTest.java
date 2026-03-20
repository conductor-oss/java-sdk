package dischargeplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateWorkerTest {

    private final CoordinateWorker worker = new CoordinateWorker();

    @Test void taskDefName() { assertEquals("dsc_coordinate", worker.getTaskDefName()); }

    @Test void arrangesServices() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P1", "services", List.of("home_health", "pharmacy"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("servicesArranged"));
    }

    @Test void confirmsServices() {
        List<String> services = List.of("a", "b", "c");
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P2", "services", services)));
        assertEquals(services, r.getOutputData().get("confirmedServices"));
    }

    @Test void handlesEmptyServices() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P3", "services", List.of())));
        assertEquals(0, r.getOutputData().get("servicesArranged"));
    }

    @Test void handlesNullServices() {
        Map<String, Object> input = new HashMap<>(); input.put("patientId", "P4"); input.put("services", null);
        TaskResult r = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(0, r.getOutputData().get("servicesArranged"));
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    @Test void singleServiceReturnsOne() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P5", "services", List.of("home_health"))));
        assertEquals(1, r.getOutputData().get("servicesArranged"));
    }

    @Test void outputContainsAllKeys() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P6", "services", List.of("x"))));
        assertTrue(r.getOutputData().containsKey("servicesArranged"));
        assertTrue(r.getOutputData().containsKey("confirmedServices"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
