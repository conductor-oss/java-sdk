package setvariable.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    private final FinalizeWorker worker = new FinalizeWorker();

    @Test
    void taskDefName() {
        assertEquals("sv_finalize", worker.getTaskDefName());
    }

    @Test
    void producesDecisionStringWithAllVariables() {
        Task task = taskWith(Map.of(
                "totalAmount", 4500.0,
                "itemCount", 3,
                "category", "high-value",
                "needsApproval", true,
                "riskLevel", "medium"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String decision = (String) result.getOutputData().get("decision");
        assertNotNull(decision);
        assertTrue(decision.contains("3 items"));
        assertTrue(decision.contains("4500.00"));
        assertTrue(decision.contains("high-value"));
        assertTrue(decision.contains("risk=medium"));
        assertTrue(decision.contains("approval=required"));
    }

    @Test
    void noApprovalDecision() {
        Task task = taskWith(Map.of(
                "totalAmount", 50.0,
                "itemCount", 2,
                "category", "micro",
                "needsApproval", false,
                "riskLevel", "low"
        ));
        TaskResult result = worker.execute(task);

        String decision = (String) result.getOutputData().get("decision");
        assertTrue(decision.contains("approval=not-required"));
        assertTrue(decision.contains("risk=low"));
        assertTrue(decision.contains("micro"));
    }

    @Test
    void outputIncludesAllFields() {
        Task task = taskWith(Map.of(
                "totalAmount", 1500.0,
                "itemCount", 5,
                "category", "high-value",
                "needsApproval", true,
                "riskLevel", "medium"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(1500.0, result.getOutputData().get("totalAmount"));
        assertEquals(5, result.getOutputData().get("itemCount"));
        assertEquals("high-value", result.getOutputData().get("category"));
        assertEquals(true, result.getOutputData().get("needsApproval"));
        assertEquals("medium", result.getOutputData().get("riskLevel"));
    }

    @Test
    void handlesDefaultsWhenInputMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String decision = (String) result.getOutputData().get("decision");
        assertNotNull(decision);
        assertTrue(decision.contains("0 items"));
        assertTrue(decision.contains("approval=not-required"));
    }

    @Test
    void decisionFormatMatchesExpectedPattern() {
        Task task = taskWith(Map.of(
                "totalAmount", 4500.0,
                "itemCount", 3,
                "category", "high-value",
                "needsApproval", true,
                "riskLevel", "medium"
        ));
        TaskResult result = worker.execute(task);

        String decision = (String) result.getOutputData().get("decision");
        assertEquals(
                "Order of 3 items ($4500.00) classified as high-value, risk=medium, approval=required",
                decision
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
