package frauddetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.tasks.Task.Status;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnalyzeTransactionWorker} -- verifies customer profile output,
 * feature extraction, in-memory history tracking, and input validation.
 */
@SuppressWarnings("unchecked")
class AnalyzeTransactionWorkerTest {

    private AnalyzeTransactionWorker worker;

    @BeforeEach
    void setUp() {
        AnalyzeTransactionWorker.resetState();
        worker = new AnalyzeTransactionWorker();
    }

    @Test
    void taskDefName() {
        assertEquals("frd_analyze_transaction", worker.getTaskDefName());
    }

    @Test
    void profileContainsCustomerId() {
        TaskResult result = execute(249.99, "MERCH-1234", "CUST-5678");

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> profile = (Map<String, Object>) result.getOutputData().get("profile");
        assertNotNull(profile);
        assertEquals("CUST-5678", profile.get("customerId"));
        assertNotNull(profile.get("accountAge"));
        assertNotNull(profile.get("avgTransactionAmount"));
        assertNotNull(profile.get("transactionCountLast30d"));
        assertNotNull(profile.get("riskTier"));
    }

    @Test
    void featuresContainExpectedKeys() {
        TaskResult result = execute(249.99, "MERCH-1234", "CUST-5678");

        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        assertNotNull(features);
        assertTrue(features.containsKey("amountDeviation"));
        assertTrue(features.containsKey("merchantCategory"));
        assertTrue(features.containsKey("timeOfDay"));
        assertTrue(features.containsKey("isNewMerchant"));
        assertTrue(features.containsKey("distanceFromHome"));
        assertTrue(features.containsKey("transactionCount"));
        assertTrue(features.containsKey("isHighRiskCategory"));
    }

    @Test
    void firstTransactionHasZeroDeviation() {
        TaskResult result = execute(100.0, "MERCH-1234", "CUST-NEW-1");

        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        double deviation = ((Number) features.get("amountDeviation")).doubleValue();
        assertEquals(0.0, deviation, 0.01);
    }

    @Test
    void secondTransactionComputesDeviationFromHistory() {
        execute(100.0, "MERCH-1234", "CUST-HIST-1");
        TaskResult result = execute(300.0, "MERCH-1234", "CUST-HIST-1");

        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        double deviation = ((Number) features.get("amountDeviation")).doubleValue();
        assertEquals(2.0, deviation, 0.01);
    }

    @Test
    void newMerchantFlaggedCorrectly() {
        TaskResult result = execute(100.0, "NEW-MERCH-99", "CUST-5678");
        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        assertEquals(true, features.get("isNewMerchant"));
    }

    @Test
    void returningMerchantNotFlagged() {
        execute(100.0, "MERCH-1234", "CUST-RETURN-1");
        TaskResult result = execute(50.0, "MERCH-1234", "CUST-RETURN-1");
        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        assertEquals(false, features.get("isNewMerchant"));
    }

    @Test
    void gamblingMerchantCategory() {
        TaskResult result = execute(100.0, "SIM_CASINO", "CUST-5678");
        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        assertEquals("gambling", features.get("merchantCategory"));
        assertEquals(true, features.get("isHighRiskCategory"));
    }

    @Test
    void cryptoMerchantIsHighRisk() {
        TaskResult result = execute(100.0, "CRYPTO_EXCHANGE", "CUST-5678");
        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        assertEquals("cryptocurrency", features.get("merchantCategory"));
        assertEquals(true, features.get("isHighRiskCategory"));
    }

    @Test
    void generalRetailMerchant() {
        TaskResult result = execute(100.0, "MERCH-1234", "CUST-5678");
        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        assertEquals("general_retail", features.get("merchantCategory"));
        assertEquals(false, features.get("isHighRiskCategory"));
    }

    @Test
    void transactionCountIncrementsWithHistory() {
        execute(100.0, "M1", "CUST-COUNT-1");
        execute(200.0, "M2", "CUST-COUNT-1");
        TaskResult result = execute(300.0, "M3", "CUST-COUNT-1");

        Map<String, Object> features = (Map<String, Object>) result.getOutputData().get("features");
        assertEquals(2, ((Number) features.get("transactionCount")).intValue());
    }

    @Test
    void newAccountRiskTier() {
        TaskResult result = execute(100.0, "MERCH-1", "CUST-BRAND-NEW");
        Map<String, Object> profile = (Map<String, Object>) result.getOutputData().get("profile");
        assertEquals("high", profile.get("riskTier"));
        assertEquals("new", profile.get("accountAge"));
    }

    // ---- Failure path: input validation ---------------------------------

    @Test
    void failsWithTerminalErrorOnNullTransactionId() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("amount", 100.0);
        input.put("merchantId", "MERCH-1");
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("transactionId"));
    }

    @Test
    void failsWithTerminalErrorOnMissingAmount() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("merchantId", "MERCH-1");
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("amount"));
    }

    @Test
    void failsWithTerminalErrorOnNegativeAmount() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("amount", -50.0);
        input.put("merchantId", "MERCH-1");
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("non-negative"));
    }

    @Test
    void failsWithTerminalErrorOnExcessiveAmount() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("amount", 999_999_999.0);
        input.put("merchantId", "MERCH-1");
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("exceeds maximum"));
    }

    @Test
    void failsWithTerminalErrorOnMissingMerchantId() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("amount", 100.0);
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("merchantId"));
    }

    @Test
    void failsWithTerminalErrorOnMissingCustomerId() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("amount", 100.0);
        input.put("merchantId", "MERCH-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("customerId"));
    }

    @Test
    void failsWithTerminalErrorOnBlankMerchantId() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("amount", 100.0);
        input.put("merchantId", "   ");
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("merchantId"));
    }

    @Test
    void failsWithTerminalErrorOnNonNumericAmount() {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-1");
        input.put("amount", "not_a_number");
        input.put("merchantId", "MERCH-1");
        input.put("customerId", "CUST-1");
        task.setInputData(input);

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("amount"));
    }

    @Test
    void zeroAmountIsValid() {
        TaskResult result = execute(0.0, "MERCH-1", "CUST-1");
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    // ---- Helper ---------------------------------------------------------

    private TaskResult execute(double amount, String merchantId, String customerId) {
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", "TXN-TEST-" + System.nanoTime());
        input.put("amount", amount);
        input.put("merchantId", merchantId);
        input.put("customerId", customerId);
        task.setInputData(input);
        return worker.execute(task);
    }
}
