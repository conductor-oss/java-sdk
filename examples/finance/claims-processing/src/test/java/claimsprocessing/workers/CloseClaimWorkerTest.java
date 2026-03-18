package claimsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CloseClaimWorkerTest {
    private final CloseClaimWorker worker = new CloseClaimWorker();

    @Test void taskDefName() { assertEquals("clp_close_claim", worker.getTaskDefName()); }

    @Test void closesClaimSuccessfully() {
        Task task = taskWith(Map.of("claimId", "CLM-100", "settledAmount", 8000, "paymentMethod", "direct_deposit"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("closed", result.getOutputData().get("claimStatus"));
    }

    @Test void outputContainsClosedDate() {
        Task task = taskWith(Map.of("settledAmount", 5000, "paymentMethod", "check"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("closedDate"));
    }

    @Test void outputContainsSurveyId() {
        Task task = taskWith(Map.of("settledAmount", 5000, "paymentMethod", "check"));
        TaskResult result = worker.execute(task);
        assertTrue(((String) result.getOutputData().get("satisfactionSurveyId")).startsWith("SRV-"));
    }

    @Test void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("closed", result.getOutputData().get("claimStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
