package kycaml.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssessRiskWorkerTest {

    private final AssessRiskWorker worker = new AssessRiskWorker();

    @Test
    void taskDefName() {
        assertEquals("kyc_assess_risk", worker.getTaskDefName());
    }

    @Test
    void lowRiskWhenVerifiedNoHits() {
        Task task = taskWith(Map.of("identityVerified", true, "watchlistHits", 0, "customerId", "CUST-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("riskLevel"));
        assertEquals(15, result.getOutputData().get("riskScore"));
    }

    @Test
    void highRiskWhenNotVerifiedWithHits() {
        Task task = taskWith(Map.of("identityVerified", false, "watchlistHits", 2, "customerId", "CUST-2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("high", result.getOutputData().get("riskLevel"));
    }

    @Test
    void mediumRiskWhenNotVerifiedNoHits() {
        Task task = taskWith(Map.of("identityVerified", false, "watchlistHits", 0, "customerId", "CUST-3"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("medium", result.getOutputData().get("riskLevel"));
        assertEquals(45, result.getOutputData().get("riskScore"));
    }

    @Test
    void handlesStringInputs() {
        Task task = taskWith(Map.of("identityVerified", "true", "watchlistHits", "0", "customerId", "CUST-4"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("riskLevel"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("identityVerified", null);
        input.put("watchlistHits", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("riskLevel"));
    }

    @Test
    void includesFactors() {
        Task task = taskWith(Map.of("identityVerified", true, "watchlistHits", 0, "customerId", "CUST-5"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("factors"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
