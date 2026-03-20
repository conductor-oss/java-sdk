package loanorigination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FundWorkerTest {

    private final FundWorker worker = new FundWorker();

    @Test
    void taskDefName() {
        assertEquals("lnr_fund", worker.getTaskDefName());
    }

    @Test
    void fundsLoan() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-001",
                "applicantId", "APP-001",
                "loanAmount", 500000,
                "approvedRate", 5.75));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("funded"));
    }

    @Test
    void returnsDisbursementId() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-002",
                "applicantId", "APP-002",
                "loanAmount", 200000,
                "approvedRate", 5.25));
        TaskResult result = worker.execute(task);

        assertEquals("DISB-88201", result.getOutputData().get("disbursementId"));
    }

    @Test
    void returnsFundedAt() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-003",
                "applicantId", "APP-003",
                "loanAmount", 100000,
                "approvedRate", 6.5));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("fundedAt"));
    }

    @Test
    void returnsFirstPaymentDate() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-004",
                "applicantId", "APP-004",
                "loanAmount", 75000,
                "approvedRate", 5.75));
        TaskResult result = worker.execute(task);

        assertEquals("2024-05-01", result.getOutputData().get("firstPaymentDate"));
    }

    @Test
    void handlesNullApplicantId() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicationId", "LOAN-005");
        input.put("applicantId", null);
        input.put("loanAmount", 50000);
        input.put("approvedRate", 5.75);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("funded"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("funded"));
    }

    @Test
    void handlesLargeAmount() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-006",
                "applicantId", "APP-006",
                "loanAmount", 5000000,
                "approvedRate", 5.25));
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
