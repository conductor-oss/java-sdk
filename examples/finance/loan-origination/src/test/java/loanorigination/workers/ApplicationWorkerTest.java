package loanorigination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationWorkerTest {

    private final ApplicationWorker worker = new ApplicationWorker();

    @Test
    void taskDefName() {
        assertEquals("lnr_application", worker.getTaskDefName());
    }

    @Test
    void receivesLoanApplication() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-001",
                "applicantId", "APP-001",
                "loanAmount", 500000,
                "loanType", "mortgage"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
    }

    @Test
    void outputContainsEmployment() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-002",
                "applicantId", "APP-002",
                "loanAmount", 200000,
                "loanType", "auto"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> employment = (Map<String, Object>) result.getOutputData().get("employment");
        assertNotNull(employment);
        assertEquals("TechCorp", employment.get("employer"));
        assertEquals(5, employment.get("years"));
        assertEquals(120000, employment.get("income"));
    }

    @Test
    void outputContainsReceivedAt() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-003",
                "applicantId", "APP-003",
                "loanAmount", 100000,
                "loanType", "personal"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    @Test
    void handlesNullApplicationId() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicationId", null);
        input.put("applicantId", "APP-004");
        input.put("loanAmount", 50000);
        input.put("loanType", "personal");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
    }

    @Test
    void handlesNullLoanType() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicationId", "LOAN-005");
        input.put("applicantId", "APP-005");
        input.put("loanAmount", 75000);
        input.put("loanType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
    }

    @Test
    void differentLoanTypesAccepted() {
        Task task = taskWith(Map.of(
                "applicationId", "LOAN-006",
                "applicantId", "APP-006",
                "loanAmount", 500000,
                "loanType", "commercial"));
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
