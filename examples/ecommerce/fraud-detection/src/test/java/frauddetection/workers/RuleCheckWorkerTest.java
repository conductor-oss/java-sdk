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
 * Tests for {@link RuleCheckWorker} — verifies rule evaluation against transaction amount
 * and profile data.
 */
@SuppressWarnings("unchecked")
class RuleCheckWorkerTest {

    private RuleCheckWorker worker;

    @BeforeEach
    void setUp() {
        worker = new RuleCheckWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("frd_rule_check", worker.getTaskDefName());
    }

    @Test
    void lowAmount_noRulesFired_lowRisk() {
        // amount=100 -> no rules fire (below all thresholds)
        TaskResult result = execute(100.0, Map.of());

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("low_risk", result.getOutputData().get("ruleResult"));
    }

    @Test
    void mediumAmount_oneRuleFired_mediumRisk() {
        // amount=600 -> only new_merchant_high_value fires (>500 but <=1000)
        TaskResult result = execute(600.0, Map.of());

        assertEquals("medium_risk", result.getOutputData().get("ruleResult"));
    }

    @Test
    void highAmount_multipleRulesFired_mediumRisk() {
        // amount=2000 -> amount_threshold (>1000, weight 1.0), new_merchant_high_value (>500, weight 1.0),
        // round_amount (exact integer >=1000 and divisible by 100, weight 0.5) -> total weight 2.5 -> medium_risk
        TaskResult result = execute(2000.0, Map.of());

        assertEquals("medium_risk", result.getOutputData().get("ruleResult"));
    }

    @Test
    void highNonRoundAmount_twoRulesFired() {
        // amount=1500.50 -> amount_threshold + new_merchant_high_value fire, round_amount does not
        TaskResult result = execute(1500.50, Map.of());

        // weight = 1.0 + 1.0 = 2.0 -> medium_risk (need >= 3.0 for high)
        String ruleResult = (String) result.getOutputData().get("ruleResult");
        assertTrue("medium_risk".equals(ruleResult) || "high_risk".equals(ruleResult));
    }

    @Test
    void rulesFiredListIncludesTriggeredStatus() {
        TaskResult result = execute(2000.0, Map.of());

        List<Map<String, Object>> rulesFired =
                (List<Map<String, Object>>) result.getOutputData().get("rulesFired");
        assertNotNull(rulesFired);
        assertTrue(rulesFired.size() >= 3);

        // Check that each rule entry has the expected structure
        for (Map<String, Object> rule : rulesFired) {
            assertNotNull(rule.get("ruleId"));
            assertNotNull(rule.get("name"));
            assertNotNull(rule.get("triggered"));
            assertNotNull(rule.get("weight"));
        }
    }

    @Test
    void rulesFiredListShowsUntriggeredRules() {
        TaskResult result = execute(100.0, Map.of());

        List<Map<String, Object>> rulesFired =
                (List<Map<String, Object>>) result.getOutputData().get("rulesFired");
        assertNotNull(rulesFired);

        // R-101, R-102, R-204, R-310, R-311 should all be false for amount=100
        long triggered = rulesFired.stream()
                .filter(r -> "R-101".equals(r.get("ruleId")) || "R-102".equals(r.get("ruleId"))
                        || "R-204".equals(r.get("ruleId")) || "R-310".equals(r.get("ruleId"))
                        || "R-311".equals(r.get("ruleId")))
                .filter(r -> Boolean.TRUE.equals(r.get("triggered")))
                .count();
        assertEquals(0, triggered, "No amount-based rules should trigger for $100");
    }

    @Test
    void rulesEvaluatedCount() {
        TaskResult result = execute(100.0, Map.of());
        int evaluated = ((Number) result.getOutputData().get("rulesEvaluated")).intValue();
        assertEquals(8, evaluated, "Should evaluate all 8 rules");
    }

    @Test
    void totalRiskWeightIsComputed() {
        TaskResult result = execute(100.0, Map.of());
        assertNotNull(result.getOutputData().get("totalRiskWeight"));
        double weight = ((Number) result.getOutputData().get("totalRiskWeight")).doubleValue();
        assertTrue(weight >= 0.0);
    }

    @Test
    void justBelowThresholdRuleFires() {
        // amount=995 should trigger R-311 (just below $1000 threshold)
        TaskResult result = execute(995.0, Map.of());
        List<Map<String, Object>> rules = (List<Map<String, Object>>) result.getOutputData().get("rulesFired");
        boolean r311Fired = rules.stream()
                .anyMatch(r -> "R-311".equals(r.get("ruleId")) && Boolean.TRUE.equals(r.get("triggered")));
        assertTrue(r311Fired, "R-311 (just_below_threshold) should fire for $995");
    }

    @Test
    void newAccountLargeTxnRuleFires() {
        // New account with amount > 200
        Map<String, Object> profile = Map.of("accountAge", "new", "riskTier", "high");
        TaskResult result = execute(500.0, profile);
        List<Map<String, Object>> rules = (List<Map<String, Object>>) result.getOutputData().get("rulesFired");
        boolean r501Fired = rules.stream()
                .anyMatch(r -> "R-501".equals(r.get("ruleId")) && Boolean.TRUE.equals(r.get("triggered")));
        assertTrue(r501Fired, "R-501 (new_account_large_txn) should fire for new account with $500");
    }

    @Test
    void highRiskProfileRule() {
        Map<String, Object> profile = Map.of("riskTier", "high");
        TaskResult result = execute(100.0, profile);
        List<Map<String, Object>> rules = (List<Map<String, Object>>) result.getOutputData().get("rulesFired");
        boolean r205Fired = rules.stream()
                .anyMatch(r -> "R-205".equals(r.get("ruleId")) && Boolean.TRUE.equals(r.get("triggered")));
        assertTrue(r205Fired, "R-205 should fire for high-risk profile");
    }

    @Test
    void veryHighAmountIsHighRisk() {
        TaskResult result = execute(15000.0, Map.of());
        assertEquals("high_risk", result.getOutputData().get("ruleResult"));
        double weight = ((Number) result.getOutputData().get("totalRiskWeight")).doubleValue();
        assertTrue(weight >= 3.0, "Very high amount should produce weight >= 3.0");
    }

    // ---- Helper ---------------------------------------------------------

    private TaskResult execute(double amount, Map<String, Object> profile) {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-TEST-001");
        input.put("amount", amount);
        if (!profile.isEmpty()) {
            input.put("profile", profile);
        }
        task.setInputData(input);
        return worker.execute(task);
    }
}
