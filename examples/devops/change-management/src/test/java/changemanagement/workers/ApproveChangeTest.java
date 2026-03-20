package changemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApproveChangeTest {

    private final ApproveChange worker = new ApproveChange();

    @Test
    void taskDefName() {
        assertEquals("cm_approve", worker.getTaskDefName());
    }

    @Test
    void approvesHighRiskWithMonitoring() {
        Task task = taskWith(Map.of("riskLevel", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
        assertEquals("cab-board", result.getOutputData().get("approver"));
        assertTrue(result.getOutputData().get("approvalNote").toString().contains("additional monitoring"));
    }

    @Test
    void approvesMediumRiskWithStandardReview() {
        Task task = taskWith(Map.of("riskLevel", "medium"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("approved"));
        assertTrue(result.getOutputData().get("approvalNote").toString().contains("standard review"));
    }

    @Test
    void autoApprovesLowRisk() {
        Task task = taskWith(Map.of("riskLevel", "low"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("approved"));
        assertTrue(result.getOutputData().get("approvalNote").toString().contains("Auto-approved"));
    }

    @Test
    void handlesUnknownRiskLevel() {
        Task task = taskWith(Map.of("riskLevel", "unknown"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("approveData", null);
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsApprover() {
        Task task = taskWith(Map.of("riskLevel", "low"));
        TaskResult result = worker.execute(task);

        assertEquals("cab-board", result.getOutputData().get("approver"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("approveData", dataMap);
        task.setInputData(input);
        return task;
    }
}
