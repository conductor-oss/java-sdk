package patientintake.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyInsuranceWorkerTest {
    private final VerifyInsuranceWorker worker = new VerifyInsuranceWorker();

    @Test void taskDefName() { assertEquals("pit_verify_insurance", worker.getTaskDefName()); }

    @Test void verifiesBlueCross() {
        TaskResult result = worker.execute(taskWith(Map.of("insuranceId", "INS-BC-55012")));
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals("Blue Cross PPO", result.getOutputData().get("plan"));
        assertEquals(30, result.getOutputData().get("copay"));
    }

    @Test void handlesMissingInsurance() {
        Map<String, Object> input = new HashMap<>(); input.put("insuranceId", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test void copayIsNumeric() {
        TaskResult result = worker.execute(taskWith(Map.of("insuranceId", "INS-BC-001")));
        assertTrue(result.getOutputData().get("copay") instanceof Number);
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
