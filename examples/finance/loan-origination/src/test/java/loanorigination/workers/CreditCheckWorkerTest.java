package loanorigination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreditCheckWorkerTest {

    private final CreditCheckWorker worker = new CreditCheckWorker();

    @Test
    void taskDefName() {
        assertEquals("lnr_credit_check", worker.getTaskDefName());
    }

    @Test
    void returnsCreditScore() {
        Task task = taskWith(Map.of("applicantId", "APP-001", "loanAmount", 500000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(742, result.getOutputData().get("creditScore"));
    }

    @Test
    void returnsBureau() {
        Task task = taskWith(Map.of("applicantId", "APP-002", "loanAmount", 200000));
        TaskResult result = worker.execute(task);

        assertEquals("Experian", result.getOutputData().get("bureau"));
    }

    @Test
    void returnsDti() {
        Task task = taskWith(Map.of("applicantId", "APP-003", "loanAmount", 100000));
        TaskResult result = worker.execute(task);

        assertEquals(28, result.getOutputData().get("dti"));
    }

    @Test
    void returnsDelinquencies() {
        Task task = taskWith(Map.of("applicantId", "APP-004", "loanAmount", 50000));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("delinquencies"));
    }

    @Test
    void returnsOpenAccounts() {
        Task task = taskWith(Map.of("applicantId", "APP-005", "loanAmount", 75000));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("openAccounts"));
    }

    @Test
    void handlesNullApplicantId() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicantId", null);
        input.put("loanAmount", 100000);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(742, result.getOutputData().get("creditScore"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(742, result.getOutputData().get("creditScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
