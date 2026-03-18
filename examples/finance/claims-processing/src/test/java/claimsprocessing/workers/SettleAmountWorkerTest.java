package claimsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SettleAmountWorkerTest {
    private final SettleAmountWorker worker = new SettleAmountWorker();

    @Test void taskDefName() { assertEquals("clp_settle_amount", worker.getTaskDefName()); }

    @Test void settlesModerateWith500Deductible() {
        Task task = taskWith(Map.of("assessedAmount", 8500, "damageCategory", "moderate"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(8000L, result.getOutputData().get("settledAmount")); // 8500 - 500
    }

    @Test void settlesMinorWith250Deductible() {
        Task task = taskWith(Map.of("assessedAmount", 1000, "damageCategory", "minor"));
        TaskResult result = worker.execute(task);
        assertEquals(750L, result.getOutputData().get("settledAmount")); // 1000 - 250
    }

    @Test void largeAmountUsesWireTransfer() {
        Task task = taskWith(Map.of("assessedAmount", 15000, "damageCategory", "major"));
        TaskResult result = worker.execute(task);
        assertEquals("wire_transfer", result.getOutputData().get("paymentMethod"));
    }

    @Test void smallAmountUsesCheck() {
        Task task = taskWith(Map.of("assessedAmount", 500, "damageCategory", "minor"));
        TaskResult result = worker.execute(task);
        assertEquals("check", result.getOutputData().get("paymentMethod"));
    }

    @Test void mediumAmountUsesDirectDeposit() {
        Task task = taskWith(Map.of("assessedAmount", 5000, "damageCategory", "moderate"));
        TaskResult result = worker.execute(task);
        assertEquals("direct_deposit", result.getOutputData().get("paymentMethod"));
    }

    @Test void settledAmountNeverNegative() {
        Task task = taskWith(Map.of("assessedAmount", 100, "damageCategory", "moderate"));
        TaskResult result = worker.execute(task);
        assertTrue(((Number) result.getOutputData().get("settledAmount")).longValue() >= 0);
    }

    @Test void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
