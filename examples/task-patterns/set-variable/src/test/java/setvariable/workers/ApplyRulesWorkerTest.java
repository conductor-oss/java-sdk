package setvariable.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyRulesWorkerTest {

    private final ApplyRulesWorker worker = new ApplyRulesWorker();

    @Test
    void taskDefName() {
        assertEquals("sv_apply_rules", worker.getTaskDefName());
    }

    @Test
    void highTotalRequiresApprovalAndMediumRisk() {
        Task task = taskWith(Map.of(
                "totalAmount", 4500.0,
                "itemCount", 3,
                "category", "high-value"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("needsApproval"));
        assertEquals("medium", result.getOutputData().get("riskLevel"));
    }

    @Test
    void veryHighTotalGivesHighRisk() {
        Task task = taskWith(Map.of(
                "totalAmount", 6000.0,
                "itemCount", 2,
                "category", "high-value"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("needsApproval"));
        assertEquals("high", result.getOutputData().get("riskLevel"));
    }

    @Test
    void lowTotalNoApprovalLowRisk() {
        Task task = taskWith(Map.of(
                "totalAmount", 50.0,
                "itemCount", 2,
                "category", "micro"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("needsApproval"));
        assertEquals("low", result.getOutputData().get("riskLevel"));
    }

    @Test
    void manyItemsTriggerApproval() {
        Task task = taskWith(Map.of(
                "totalAmount", 200.0,
                "itemCount", 15,
                "category", "standard"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("needsApproval"));
        assertEquals("low", result.getOutputData().get("riskLevel"));
    }

    @Test
    void boundaryAt500DoesNotTriggerApproval() {
        Task task = taskWith(Map.of(
                "totalAmount", 500.0,
                "itemCount", 5,
                "category", "standard"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("needsApproval"));
        assertEquals("low", result.getOutputData().get("riskLevel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
