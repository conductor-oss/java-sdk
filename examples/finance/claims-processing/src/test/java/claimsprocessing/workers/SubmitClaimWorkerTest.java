package claimsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SubmitClaimWorkerTest {
    private final SubmitClaimWorker worker = new SubmitClaimWorker();

    @Test void taskDefName() { assertEquals("clp_submit_claim", worker.getTaskDefName()); }

    @Test void submitsClaimSuccessfully() {
        Task task = taskWith(Map.of("claimId", "CLM-100", "policyId", "POL-200",
                "claimType", "auto_collision", "description", "Fender bender", "amount", 5000));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("active", result.getOutputData().get("policyStatus"));
    }

    @Test void outputContainsSubmissionDate() {
        Task task = taskWith(Map.of("claimId", "CLM-101", "policyId", "POL-201"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("submissionDate"));
    }

    @Test void outputContainsReferenceNumber() {
        Task task = taskWith(Map.of("claimId", "CLM-102", "policyId", "POL-202"));
        TaskResult result = worker.execute(task);
        assertTrue(((String)result.getOutputData().get("referenceNumber")).startsWith("CLM-"));
    }

    @Test void handlesNullClaimId() {
        Map<String, Object> input = new HashMap<>();
        input.put("claimId", null); input.put("policyId", "POL-300");
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("policyStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
