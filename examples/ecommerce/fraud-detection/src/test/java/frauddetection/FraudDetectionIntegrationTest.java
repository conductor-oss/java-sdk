package frauddetection;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import frauddetection.workers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full fraud detection workflow data flow.
 * Verifies that worker A output feeds worker B input correctly across the pipeline:
 *   AnalyzeTransaction -> (RuleCheck, MlScore, VelocityCheck) -> Decide
 */
@SuppressWarnings("unchecked")
class FraudDetectionIntegrationTest {

    private AnalyzeTransactionWorker analyzeWorker;
    private RuleCheckWorker ruleCheckWorker;
    private MlScoreWorker mlScoreWorker;
    private VelocityCheckWorker velocityCheckWorker;
    private DecideWorker decideWorker;

    @BeforeEach
    void setUp() {
        AnalyzeTransactionWorker.resetState();
        VelocityCheckWorker.resetState();
        analyzeWorker = new AnalyzeTransactionWorker();
        ruleCheckWorker = new RuleCheckWorker();
        mlScoreWorker = new MlScoreWorker();
        velocityCheckWorker = new VelocityCheckWorker();
        decideWorker = new DecideWorker();
    }

    @Test
    void fullPipeline_lowRiskTransaction_approvesTransaction() {
        // Step 1: Analyze transaction
        TaskResult analyzeResult = executeAnalyze("TXN-SAFE-001", 50.0, "GROC-STORE-1", "CUST-TRUSTED");
        assertEquals(TaskResult.Status.COMPLETED, analyzeResult.getStatus());

        Map<String, Object> profile = (Map<String, Object>) analyzeResult.getOutputData().get("profile");
        Map<String, Object> features = (Map<String, Object>) analyzeResult.getOutputData().get("features");
        assertNotNull(profile);
        assertNotNull(features);

        // Step 2a: Rule check (uses analyze output: amount + profile)
        TaskResult ruleResult = executeRuleCheck("TXN-SAFE-001", 50.0, profile);
        assertEquals(TaskResult.Status.COMPLETED, ruleResult.getStatus());
        String ruleResultStr = (String) ruleResult.getOutputData().get("ruleResult");
        assertNotNull(ruleResultStr);

        // Step 2b: ML score (uses analyze output: features)
        TaskResult mlResult = executeMlScore("TXN-SAFE-001", features);
        assertEquals(TaskResult.Status.COMPLETED, mlResult.getStatus());
        double fraudScore = ((Number) mlResult.getOutputData().get("fraudScore")).doubleValue();
        assertTrue(fraudScore >= 0 && fraudScore <= 1.0);

        // Step 2c: Velocity check (uses customerId + transactionId)
        TaskResult velResult = executeVelocity("CUST-TRUSTED", "TXN-SAFE-001");
        assertEquals(TaskResult.Status.COMPLETED, velResult.getStatus());
        String velocityResultStr = (String) velResult.getOutputData().get("velocityResult");
        assertNotNull(velocityResultStr);

        // Step 3: Decide (combines all results)
        TaskResult decision = executeDecide("TXN-SAFE-001", ruleResultStr, fraudScore, velocityResultStr);
        assertEquals(TaskResult.Status.COMPLETED, decision.getStatus());
        assertEquals("APPROVE", decision.getOutputData().get("decision"),
                "Low-risk $50 grocery transaction should be approved");
    }

    @Test
    void fullPipeline_highRiskTransaction_blocksTransaction() {
        // Step 1: Analyze a high-risk transaction (high-risk merchant + high amount)
        TaskResult analyzeResult = executeAnalyze("TXN-RISKY-001", 15000.0, "CRYPTO_EXCHANGE", "CUST-NEW");
        assertEquals(TaskResult.Status.COMPLETED, analyzeResult.getStatus());

        Map<String, Object> profile = (Map<String, Object>) analyzeResult.getOutputData().get("profile");
        Map<String, Object> features = (Map<String, Object>) analyzeResult.getOutputData().get("features");

        // Verify the features correctly identify the high-risk category
        assertEquals(true, features.get("isHighRiskCategory"));
        assertEquals("cryptocurrency", features.get("merchantCategory"));

        // Step 2a: Rule check
        TaskResult ruleResult = executeRuleCheck("TXN-RISKY-001", 15000.0, profile);
        String ruleResultStr = (String) ruleResult.getOutputData().get("ruleResult");
        assertEquals("high_risk", ruleResultStr, "High amount + new account should be high_risk");

        // Step 2b: ML score
        TaskResult mlResult = executeMlScore("TXN-RISKY-001", features);
        double fraudScore = ((Number) mlResult.getOutputData().get("fraudScore")).doubleValue();
        assertTrue(fraudScore > 0.3, "High-risk crypto transaction should get elevated ML score");

        // Step 2c: Velocity check
        TaskResult velResult = executeVelocity("CUST-NEW", "TXN-RISKY-001");
        String velocityResultStr = (String) velResult.getOutputData().get("velocityResult");

        // Step 3: Decide
        TaskResult decision = executeDecide("TXN-RISKY-001", ruleResultStr, fraudScore, velocityResultStr);
        assertEquals(TaskResult.Status.COMPLETED, decision.getStatus());
        String decisionStr = (String) decision.getOutputData().get("decision");
        assertTrue("BLOCK".equals(decisionStr) || "REVIEW".equals(decisionStr),
                "High-risk $15K crypto transaction should be blocked or reviewed, got: " + decisionStr);
    }

    @Test
    void fullPipeline_invalidInput_failsAtFirstStep() {
        // Negative amount should fail at analyze step
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-BAD");
        input.put("amount", -100.0);
        input.put("merchantId", "MERCH-1");
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = analyzeWorker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        // The pipeline stops here -- downstream workers never execute
    }

    @Test
    void profileDataFlowsCorrectlyToRuleCheck() {
        // Build up transaction history to get a non-"new" account age
        for (int i = 0; i < 12; i++) {
            executeAnalyze("TXN-HIST-" + i, 100.0, "MERCH-" + i, "CUST-HISTORY");
        }
        TaskResult analyzeResult = executeAnalyze("TXN-HIST-FINAL", 100.0, "MERCH-X", "CUST-HISTORY");
        Map<String, Object> profile = (Map<String, Object>) analyzeResult.getOutputData().get("profile");

        // Verify profile age has progressed past "new"
        assertEquals("regular", profile.get("accountAge"));
        assertEquals("standard", profile.get("riskTier"));

        // Rule check should NOT fire R-501 (new account + large txn) since account is "regular"
        TaskResult ruleResult = executeRuleCheck("TXN-HIST-FINAL", 500.0, profile);
        assertEquals("medium_risk", ruleResult.getOutputData().get("ruleResult"));
    }

    // ---- Helpers --------------------------------------------------------

    private TaskResult executeAnalyze(String txnId, double amount, String merchantId, String customerId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "transactionId", txnId, "amount", amount,
                "merchantId", merchantId, "customerId", customerId)));
        return analyzeWorker.execute(task);
    }

    private TaskResult executeRuleCheck(String txnId, double amount, Map<String, Object> profile) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", txnId);
        input.put("amount", amount);
        input.put("profile", profile);
        task.setInputData(input);
        return ruleCheckWorker.execute(task);
    }

    private TaskResult executeMlScore(String txnId, Map<String, Object> features) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("transactionId", txnId, "features", features)));
        return mlScoreWorker.execute(task);
    }

    private TaskResult executeVelocity(String customerId, String txnId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("customerId", customerId, "transactionId", txnId)));
        return velocityCheckWorker.execute(task);
    }

    private TaskResult executeDecide(String txnId, String ruleResult, double mlScore, String velocityResult) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "transactionId", txnId, "ruleResult", ruleResult,
                "mlScore", mlScore, "velocityResult", velocityResult)));
        return decideWorker.execute(task);
    }
}
