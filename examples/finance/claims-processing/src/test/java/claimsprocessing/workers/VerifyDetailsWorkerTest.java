package claimsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyDetailsWorkerTest {
    private final VerifyDetailsWorker worker = new VerifyDetailsWorker();

    @Test void taskDefName() { assertEquals("clp_verify_details", worker.getTaskDefName()); }

    @Test void verifiesPolicySuccessfully() {
        Task task = taskWith(Map.of("claimId", "CLM-100", "policyId", "POL-200", "policyStatus", "active"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    @Test void failsWhenPolicyNotActive() {
        Task task = taskWith(Map.of("policyId", "POL-201", "policyStatus", "expired"));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("verified"));
    }

    @Test void outputContainsCoverageLimit() {
        Task task = taskWith(Map.of("policyId", "POL-202", "policyStatus", "active"));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().get("coverageLimit") instanceof Number);
        assertTrue(((Number) result.getOutputData().get("coverageLimit")).intValue() > 0);
    }

    @Test void outputContainsDeductible() {
        Task task = taskWith(Map.of("policyId", "POL-203", "policyStatus", "active"));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().get("deductible") instanceof Number);
        assertTrue(((Number) result.getOutputData().get("deductible")).intValue() > 0);
    }

    @Test void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
