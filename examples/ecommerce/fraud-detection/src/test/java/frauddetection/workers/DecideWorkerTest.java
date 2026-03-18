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
 * Tests for {@link DecideWorker} — verifies the APPROVE / REVIEW / BLOCK
 * decision logic using the weighted ensemble approach.
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
        // combined = 0.4*0.1 + 0.3*0 + 0.3*0 = 0.04
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
        // combined = 0.4*0.2 + 0.3*0 + 0.3*0 = 0.08
        TaskResult result = execute(0.2, "low_risk", "normal");
        assertEquals("APPROVE", result.getOutputData().get("decision"));
    }

    // ---- REVIEW scenarios -----------------------------------------------

    @Test
    void review_mediumRiskRules() {
        // combined = 0.4*0.1 + 0.3*0.5 + 0.3*0 = 0.19 -> APPROVE
        // Actually: 0.04 + 0.15 = 0.19 < 0.4 threshold, so this would be APPROVE
        // Need medium_risk + elevated velocity:
        // combined = 0.4*0.3 + 0.3*0.5 + 0.3*0.5 = 0.12 + 0.15 + 0.15 = 0.42 -> REVIEW
        TaskResult result = execute(0.3, "medium_risk", "elevated");
        assertEquals("REVIEW", result.getOutputData().get("decision"));
    }

    @Test
    void review_moderateMLScore() {
        // combined = 0.4*0.6 + 0.3*0.5 + 0.3*0 = 0.24 + 0.15 = 0.39 -> still approve
        // Need higher: 0.4*0.8 + 0.3*0 + 0.3*0.5 = 0.32 + 0 + 0.15 = 0.47 -> REVIEW
        TaskResult result = execute(0.8, "low_risk", "elevated");
        assertEquals("REVIEW", result.getOutputData().get("decision"));
    }

    // ---- BLOCK scenarios ------------------------------------------------

    @Test
    void block_overrideHighMLAndHighRiskRules() {
        // Override: mlScore > 0.8 AND ruleResult = high_risk -> instant BLOCK
        TaskResult result = execute(0.9, "high_risk", "normal");

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("BLOCK", result.getOutputData().get("decision"));
        assertTrue(((Number) result.getOutputData().get("riskScore")).doubleValue() >= 0.85);
    }

    @Test
    void block_overrideSuspiciousVelocityHighML() {
        // Override: velocity=suspicious AND mlScore > 0.5 -> instant BLOCK
        TaskResult result = execute(0.6, "low_risk", "suspicious");
        assertEquals("BLOCK", result.getOutputData().get("decision"));
    }

    @Test
    void block_highCombinedScore() {
        // combined = 0.4*0.9 + 0.3*1.0 + 0.3*1.0 = 0.36 + 0.3 + 0.3 = 0.96 -> BLOCK
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

    // ---- Edge cases -----------------------------------------------------

    @Test
    void nullMlScore_treatsAsZero() {
        TaskResult result = execute(null, "low_risk", "normal");
        assertEquals("APPROVE", result.getOutputData().get("decision"));
        assertEquals(0.0, ((Number) result.getOutputData().get("combinedScore")).doubleValue());
    }

    @Test
    void nullTransactionId_doesNotFail() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "ruleResult", "low_risk",
                "mlScore", 0.1,
                "velocityResult", "normal"
        )));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("APPROVE", result.getOutputData().get("decision"));
    }

    // ---- Helper ---------------------------------------------------------

    private TaskResult execute(Object mlScore, String ruleResult, String velocityResult) {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-TEST-001");
        input.put("ruleResult", ruleResult);
        if (mlScore != null) {
            input.put("mlScore", mlScore);
        }
        input.put("velocityResult", velocityResult);
        task.setInputData(input);
        return worker.execute(task);
    }
}
