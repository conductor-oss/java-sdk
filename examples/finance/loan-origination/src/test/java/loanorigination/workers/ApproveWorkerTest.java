package loanorigination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApproveWorkerTest {

    private final ApproveWorker worker = new ApproveWorker();

    @Test
    void taskDefName() {
        assertEquals("lnr_approve", worker.getTaskDefName());
    }

    @Test
    void approvesLoan() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-001",
                "underwritingDecision", "approved",
                "approvedRate", 5.75));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void returnsApprovalNumber() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-002",
                "underwritingDecision", "approved",
                "approvedRate", 5.25));
        TaskResult result = worker.execute(task);

        assertEquals("APR-LN-44201", result.getOutputData().get("approvalNumber"));
    }

    @Test
    void handlesDeclinedDecision() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-003",
                "underwritingDecision", "declined",
                "approvedRate", 6.5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesNullApplicationId() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicationId", null);
        input.put("underwritingDecision", "approved");
        input.put("approvedRate", 5.75);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test
    void handlesNumericRate() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-005",
                "underwritingDecision", "approved",
                "approvedRate", 5.25));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesStringRate() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-006",
                "underwritingDecision", "approved",
                "approvedRate", "5.75"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
