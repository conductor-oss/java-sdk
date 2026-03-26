package frauddetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MlScoreWorker} -- verifies scoring model and input validation.
 */
@SuppressWarnings("unchecked")
class MlScoreWorkerTest {

    private final MlScoreWorker worker = new MlScoreWorker();

    @Test
    void taskDefName() {
        assertEquals("frd_ml_score", worker.getTaskDefName());
    }

    @Test
    void scoresWithFullFeatures() {
        Map<String, Object> features = new HashMap<>(Map.of(
                "amountDeviation", 1.0,
                "distanceFromHome", 100.0,
                "isNewMerchant", true,
                "timeOfDay", "night",
                "isHighRiskCategory", true,
                "accountAge", "new",
                "transactionCount", 0
        ));
        TaskResult result = execute("TXN-1", features);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        double score = ((Number) result.getOutputData().get("fraudScore")).doubleValue();
        assertTrue(score > 0.0 && score <= 1.0);
        assertEquals(7, ((Number) result.getOutputData().get("featuresUsed")).intValue());
    }

    @Test
    void lowRiskFeaturesProduceLowScore() {
        Map<String, Object> features = new HashMap<>(Map.of(
                "amountDeviation", 0.0,
                "distanceFromHome", 0.0,
                "isNewMerchant", false,
                "timeOfDay", "morning",
                "isHighRiskCategory", false,
                "accountAge", "established",
                "transactionCount", 50
        ));
        TaskResult result = execute("TXN-2", features);
        double score = ((Number) result.getOutputData().get("fraudScore")).doubleValue();
        assertEquals(0.0, score, 0.01);
    }

    @Test
    void failsWithTerminalErrorOnMissingTransactionId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("features", Map.of())));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("transactionId"));
    }

    @Test
    void failsWithTerminalErrorOnMissingFeatures() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("transactionId", "TXN-1")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("features"));
    }

    @Test
    void failsWithTerminalErrorOnNonMapFeatures() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("transactionId", "TXN-1", "features", "not_a_map")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("features"));
    }

    @Test
    void includesFeatureImportance() {
        Map<String, Object> features = new HashMap<>(Map.of(
                "amountDeviation", 2.0, "transactionCount", 5));
        TaskResult result = execute("TXN-3", features);
        Map<String, Object> importance = (Map<String, Object>) result.getOutputData().get("featureImportance");
        assertNotNull(importance);
        assertTrue(importance.containsKey("amountDeviation"));
    }

    private TaskResult execute(String transactionId, Map<String, Object> features) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("transactionId", transactionId);
        input.put("features", features);
        task.setInputData(input);
        return worker.execute(task);
    }
}
