package changemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssessRiskTest {

    private final AssessRisk worker = new AssessRisk();

    @Test
    void taskDefName() {
        assertEquals("cm_assess_risk", worker.getTaskDefName());
    }

    @Test
    void assessesEmergencyAsHighRisk() {
        Task task = taskWith(Map.of("changeType", "emergency"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("high", result.getOutputData().get("riskLevel"));
        assertEquals(85, result.getOutputData().get("riskScore"));
        assertEquals(true, result.getOutputData().get("requiresApproval"));
    }

    @Test
    void assessesNormalAsMediumRisk() {
        Task task = taskWith(Map.of("changeType", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals("medium", result.getOutputData().get("riskLevel"));
        assertEquals(50, result.getOutputData().get("riskScore"));
        assertEquals(true, result.getOutputData().get("requiresApproval"));
    }

    @Test
    void assessesStandardAsLowRisk() {
        Task task = taskWith(Map.of("changeType", "standard"));
        TaskResult result = worker.execute(task);

        assertEquals("low", result.getOutputData().get("riskLevel"));
        assertEquals(25, result.getOutputData().get("riskScore"));
        assertEquals(false, result.getOutputData().get("requiresApproval"));
    }

    @Test
    void assessesUnknownTypeAsLowRisk() {
        Task task = taskWith(Map.of("changeType", "custom"));
        TaskResult result = worker.execute(task);

        assertEquals("low", result.getOutputData().get("riskLevel"));
        assertEquals(25, result.getOutputData().get("riskScore"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("assess_riskData", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low", result.getOutputData().get("riskLevel"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("assessed"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("changeType", "normal"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("riskLevel"));
        assertNotNull(result.getOutputData().get("riskScore"));
        assertNotNull(result.getOutputData().get("assessed"));
        assertNotNull(result.getOutputData().get("requiresApproval"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("assess_riskData", dataMap);
        task.setInputData(input);
        return task;
    }
}
