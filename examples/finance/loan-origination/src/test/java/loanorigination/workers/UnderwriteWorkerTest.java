package loanorigination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnderwriteWorkerTest {

    private final UnderwriteWorker worker = new UnderwriteWorker();

    @Test
    void taskDefName() {
        assertEquals("lnr_underwrite", worker.getTaskDefName());
    }

    @Test
    void approvesHighScoreLowDti() {
        Task task = taskWith(Map.of("creditScore", 742, "dti", 28, "applicationId", "LOAN-001", "loanAmount", 500000));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("approved", result.getOutputData().get("decision"));
        assertEquals(5.75, result.getOutputData().get("interestRate"));
    }

    @Test
    void approvesExcellentScoreWithLowRate() {
        Task task = taskWith(Map.of("creditScore", 820, "dti", 20, "applicationId", "LOAN-002", "loanAmount", 200000));
        TaskResult result = worker.execute(task);

        assertEquals("approved", result.getOutputData().get("decision"));
        assertEquals(5.25, result.getOutputData().get("interestRate"));
    }

    @Test
    void declinesLowScore() {
        Task task = taskWith(Map.of("creditScore", 600, "dti", 50, "applicationId", "LOAN-003", "loanAmount", 100000));
        TaskResult result = worker.execute(task);

        assertEquals("declined", result.getOutputData().get("decision"));
    }

    @Test
    void declinesHighDti() {
        Task task = taskWith(Map.of("creditScore", 720, "dti", 50, "applicationId", "LOAN-004", "loanAmount", 100000));
        TaskResult result = worker.execute(task);

        assertEquals("declined", result.getOutputData().get("decision"));
    }

    @Test
    void borderlineScoreApproved() {
        Task task = taskWith(Map.of("creditScore", 680, "dti", 43, "applicationId", "LOAN-005", "loanAmount", 100000));
        TaskResult result = worker.execute(task);

        assertEquals("approved", result.getOutputData().get("decision"));
        assertEquals(6.5, result.getOutputData().get("interestRate"));
    }

    @Test
    void borderlineScoreDeclined() {
        Task task = taskWith(Map.of("creditScore", 679, "dti", 43, "applicationId", "LOAN-006", "loanAmount", 100000));
        TaskResult result = worker.execute(task);

        assertEquals("declined", result.getOutputData().get("decision"));
    }

    @Test
    void outputContainsTerm() {
        Task task = taskWith(Map.of("creditScore", 742, "dti", 28, "applicationId", "LOAN-007", "loanAmount", 500000));
        TaskResult result = worker.execute(task);

        assertEquals(360, result.getOutputData().get("term"));
    }

    @Test
    void handlesNullCreditScore() {
        Map<String, Object> input = new HashMap<>();
        input.put("creditScore", null);
        input.put("dti", 28);
        input.put("applicationId", "LOAN-008");
        input.put("loanAmount", 100000);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("declined", result.getOutputData().get("decision"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("declined", result.getOutputData().get("decision"));
    }

    @Test
    void handlesStringCreditScore() {
        Task task = taskWith(Map.of("creditScore", "742", "dti", "28", "applicationId", "LOAN-009", "loanAmount", 500000));
        TaskResult result = worker.execute(task);

        assertEquals("approved", result.getOutputData().get("decision"));
        assertEquals(5.75, result.getOutputData().get("interestRate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
