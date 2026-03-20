package dischargeplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateDischargePlanWorkerTest {

    private final CreateDischargePlanWorker worker = new CreateDischargePlanWorker();

    @Test void taskDefName() { assertEquals("dsc_create_plan", worker.getTaskDefName()); }

    @Test void returnsServices() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P1", "readiness", "ready", "needs", List.of("a"))));
        @SuppressWarnings("unchecked") List<String> services = (List<String>) r.getOutputData().get("services");
        assertEquals(3, services.size());
    }

    @Test void returnsMedications() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P2", "readiness", "ready", "needs", List.of("a", "b"))));
        @SuppressWarnings("unchecked") List<?> meds = (List<?>) r.getOutputData().get("medications");
        assertEquals(2, meds.size());
    }

    @Test void returnsFollowUpNeeds() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P3", "readiness", "ready", "needs", List.of())));
        @SuppressWarnings("unchecked") List<String> followUp = (List<String>) r.getOutputData().get("followUpNeeds");
        assertEquals(2, followUp.size());
        assertTrue(followUp.contains("pcp_1wk"));
    }

    @Test void handlesNullNeeds() {
        Map<String, Object> input = new HashMap<>(); input.put("patientId", "P4"); input.put("readiness", "ready"); input.put("needs", null);
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(input)).getStatus());
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    @Test void servicesContainsHomeHealth() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P5", "readiness", "ready", "needs", List.of())));
        @SuppressWarnings("unchecked") List<String> services = (List<String>) r.getOutputData().get("services");
        assertTrue(services.contains("home_health"));
    }

    @Test void outputContainsAllKeys() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P6", "readiness", "ready", "needs", List.of("x"))));
        assertTrue(r.getOutputData().containsKey("services"));
        assertTrue(r.getOutputData().containsKey("medications"));
        assertTrue(r.getOutputData().containsKey("followUpNeeds"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
