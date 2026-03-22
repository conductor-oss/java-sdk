package frauddetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.Task.Status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DecideWorker} -- verifies the APPROVE / REVIEW / BLOCK
 * decision logic using the weighted ensemble approach, plus failure-path validation.
 */
@SuppressWarnings("unchecked")
class DecideWorkerTest {

    private DecideWorker worker;

    @BeforeEach
    void setUp() {
        worker = new DecideWorker();
    }

    // ---- Task-def name --------------------------------------------------

    @Test
    void taskDefName() {
        assertEquals("frd_decide", worker.getTaskDefName());
    }

    // ---- APPROVE scenarios ----------------------------------------------

    @Test
    void approve_lowScore_lowRisk_normal() {
        TaskResult result = execute(0.1, "low_risk", "normal");
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("APPROVE", result.getOutputData().get("decision"));
        assertNotNull(result.getOutputData().get("reason"));
        assertNotNull(result.getOutputData().get("combinedScore"));
    }

    @Test
    void approve_zeroScore_lowRisk_normal() {
        TaskResult result = execute(0.0, "low_risk", "normal");
        assertEquals("APPROVE", result.getOutputData().get("decision"));
        assertEquals(0.0, ((Number) result.getOutputData().get("combinedScore")).doubleValue());
    }

    @Test
    void approve_allSignalsLow() {
        TaskResult result = execute(0.2, "low_risk", "normal");
        assertEquals("APPROVE", result.getOutputData().get("decision"));
    }

    // ---- REVIEW scenarios -----------------------------------------------

    @Test
    void review_mediumRiskRules() {
        TaskResult result = execute(0.3, "medium_risk", "elevated");
        assertEquals("REVIEW", result.getOutputData().get("decision"));
    }

    @Test
    void review_moderateMLScore() {
        TaskResult result = execute(0.8, "low_risk", "elevated");
        assertEquals("REVIEW", result.getOutputData().get("decision"));
    }

    // ---- BLOCK scenarios ------------------------------------------------

    @Test
    void block_overrideHighMLAndHighRiskRules() {
        TaskResult result = execute(0.9, "high_risk", "normal");
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("BLOCK", result.getOutputData().get("decision"));
        assertTrue(((Number) result.getOutputData().get("riskScore")).doubleValue() >= 0.85);
    }

    @Test
    void block_overrideSuspiciousVelocityHighML() {
        TaskResult result = execute(0.6, "low_risk", "suspicious");
        assertEquals("BLOCK", result.getOutputData().get("decision"));
    }

    @Test
    void block_highCombinedScore() {
        TaskResult result = execute(0.9, "high_risk", "suspicious");
        assertEquals("BLOCK", result.getOutputData().get("decision"));
    }

    @Test
    void block_allSignalsHigh() {
        TaskResult result = execute(0.95, "high_risk", "suspicious");
        assertEquals("BLOCK", result.getOutputData().get("decision"));
        assertTrue(((Number) result.getOutputData().get("riskScore")).doubleValue() >= 0.85);
    }

    // ---- Component scores and signals -----------------------------------

    @Test
    void includesComponentScores() {
        TaskResult result = execute(0.5, "medium_risk", "elevated");
        Map<String, Object> scores = (Map<String, Object>) result.getOutputData().get("componentScores");
        assertNotNull(scores);
        assertEquals(0.5, ((Number) scores.get("mlScore")).doubleValue());
        assertEquals(0.5, ((Number) scores.get("ruleScore")).doubleValue());
        assertEquals(0.5, ((Number) scores.get("velocityScore")).doubleValue());
    }

    @Test
    void includesSignalsList() {
        TaskResult result = execute(0.6, "medium_risk", "normal");
        List<String> signals = (List<String>) result.getOutputData().get("signals");
        assertNotNull(signals);
        assertTrue(signals.stream().anyMatch(s -> s.contains("ML fraud score")));
        assertTrue(signals.stream().anyMatch(s -> s.contains("Medium-risk")));
    }

    // ---- Failure path: input validation ---------------------------------

    @Test
    void failsWithTerminalErrorOnMissingTransactionId() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("ruleResult", "low_risk");
        input.put("mlScore", 0.1);
        input.put("velocityResult", "normal");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("transactionId"));
    }

    @Test
    void failsWithTerminalErrorOnMissingMlScore() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("ruleResult", "low_risk");
        input.put("velocityResult", "normal");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("mlScore"));
    }

    @Test
    void failsWithTerminalErrorOnMissingRuleResult() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("mlScore", 0.1);
        input.put("velocityResult", "normal");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("ruleResult"));
    }

    @Test
    void failsWithTerminalErrorOnMissingVelocityResult() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("ruleResult", "low_risk");
        input.put("mlScore", 0.1);
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("velocityResult"));
    }

    @Test
    void failsWithTerminalErrorOnInvalidRuleResult() {
        TaskResult result = execute(0.1, "garbage_value", "normal");
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("Invalid ruleResult"));
    }

    @Test
    void failsWithTerminalErrorOnInvalidVelocityResult() {
        TaskResult result = execute(0.1, "low_risk", "garbage_value");
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("Invalid velocityResult"));
    }

    @Test
    void failsWithTerminalErrorOnMlScoreOutOfRange() {
        TaskResult result = execute(1.5, "low_risk", "normal");
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("between 0 and 1.0"));
    }

    @Test
    void failsWithTerminalErrorOnNegativeMlScore() {
        TaskResult result = execute(-0.1, "low_risk", "normal");
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("between 0 and 1.0"));
    }

    // ---- Helper ---------------------------------------------------------

    private TaskResult execute(double mlScore, String ruleResult, String velocityResult) {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-TEST-001");
        input.put("ruleResult", ruleResult);
        input.put("mlScore", mlScore);
        input.put("velocityResult", velocityResult);
        task.setInputData(input);
        return worker.execute(task);
    }
}
