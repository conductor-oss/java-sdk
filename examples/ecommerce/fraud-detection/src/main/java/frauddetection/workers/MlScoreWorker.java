package frauddetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scores the transaction using a real weighted fraud-detection model.
 *
 * The model computes a fraud score from the feature vector produced by
 * AnalyzeTransactionWorker using the following weighted factors:
 *
 *   Factor                Weight  Scoring
 *   ─────────────────────────────────────────────────────────
 *   amountDeviation       0.25    deviation/5.0 (capped at 1.0) -- high deviations = risk
 *   distanceFromHome      0.15    distance/500.0 (capped at 1.0) -- farther = more risk
 *   isNewMerchant         0.15    1.0 if new, 0.0 if known
 *   timeOfDay             0.10    1.0 for "night", 0.3 for "evening", else 0.0
 *   isHighRiskCategory    0.15    1.0 if gambling/crypto/etc, else 0.0
 *   accountAge            0.10    1.0 for "new", 0.3 for "regular", 0.0 for "established"
 *   transactionCount      0.10    inverse: max(0, 1 - count/20) -- fewer transactions = more risk
 *
 * Confidence is calculated based on how many features are present (more data = higher confidence).
 *
 * Input: transactionId, features
 * Output: fraudScore, modelVersion, confidence, featureImportance
 */
public class MlScoreWorker implements Worker {

    private static final String MODEL_VERSION = "fraud-model-v4.1";

    @Override
    public String getTaskDefName() {
        return "frd_ml_score";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String transactionId = (String) task.getInputData().get("transactionId");
        if (transactionId == null) transactionId = "UNKNOWN";

        System.out.println("  [ml_score] Scoring transaction " + transactionId + " with " + MODEL_VERSION);

        // --- Extract features (defensive defaults) ---
        Map<String, Object> features = Map.of();
        Object featuresObj = task.getInputData().get("features");
        if (featuresObj instanceof Map) {
            features = (Map<String, Object>) featuresObj;
        }

        double amountDev = toDouble(features.get("amountDeviation"), 0.0);
        double distance = toDouble(features.get("distanceFromHome"), 0.0);
        boolean isNewMerchant = Boolean.TRUE.equals(features.get("isNewMerchant"));
        boolean isHighRiskCategory = Boolean.TRUE.equals(features.get("isHighRiskCategory"));
        String timeOfDay = String.valueOf(features.getOrDefault("timeOfDay", ""));
        String accountAge = String.valueOf(features.getOrDefault("accountAge", "regular"));
        int txCount = features.get("transactionCount") instanceof Number
                ? ((Number) features.get("transactionCount")).intValue() : 5;

        // --- Weighted scoring model ---
        int featuresPresent = 0;

        // Factor 1: Amount deviation (weight 0.25)
        double devScore = Math.min(amountDev / 5.0, 1.0);
        double f1 = devScore * 0.25;
        if (features.containsKey("amountDeviation")) featuresPresent++;

        // Factor 2: Distance from home (weight 0.15)
        double distScore = Math.min(distance / 500.0, 1.0);
        double f2 = distScore * 0.15;
        if (features.containsKey("distanceFromHome")) featuresPresent++;

        // Factor 3: New merchant (weight 0.15)
        double f3 = isNewMerchant ? 0.15 : 0.0;
        if (features.containsKey("isNewMerchant")) featuresPresent++;

        // Factor 4: Time of day (weight 0.10)
        double timeScore = "night".equalsIgnoreCase(timeOfDay) ? 1.0
                : "evening".equalsIgnoreCase(timeOfDay) ? 0.3 : 0.0;
        double f4 = timeScore * 0.10;
        if (features.containsKey("timeOfDay")) featuresPresent++;

        // Factor 5: High-risk category (weight 0.15)
        double f5 = isHighRiskCategory ? 0.15 : 0.0;
        if (features.containsKey("isHighRiskCategory") || features.containsKey("merchantCategory")) featuresPresent++;

        // Factor 6: Account age (weight 0.10)
        double ageScore = "new".equals(accountAge) ? 1.0
                : "regular".equals(accountAge) ? 0.3 : 0.0;
        double f6 = ageScore * 0.10;
        if (features.containsKey("accountAge")) featuresPresent++;

        // Factor 7: Transaction count (weight 0.10) - fewer transactions = more risk
        double countScore = Math.max(0.0, 1.0 - txCount / 20.0);
        double f7 = countScore * 0.10;
        if (features.containsKey("transactionCount")) featuresPresent++;

        double score = f1 + f2 + f3 + f4 + f5 + f6 + f7;
        score = Math.max(0.0, Math.min(1.0, score));
        score = Math.round(score * 100.0) / 100.0;

        // Confidence is based on how many features are available
        double confidence = Math.min(0.99, 0.5 + (featuresPresent / 7.0) * 0.49);
        confidence = Math.round(confidence * 100.0) / 100.0;

        System.out.println("  [ml_score] Transaction " + transactionId + ": score=" + score
                + ", confidence=" + confidence + " (" + featuresPresent + "/7 features)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fraudScore", score);
        result.getOutputData().put("modelVersion", MODEL_VERSION);
        result.getOutputData().put("confidence", confidence);
        result.getOutputData().put("featuresUsed", featuresPresent);

        Map<String, Object> featureImportance = new LinkedHashMap<>();
        featureImportance.put("amountDeviation", Map.of("weight", 0.25, "rawScore", devScore, "contribution", f1));
        featureImportance.put("distanceFromHome", Map.of("weight", 0.15, "rawScore", distScore, "contribution", f2));
        featureImportance.put("isNewMerchant", Map.of("weight", 0.15, "rawScore", isNewMerchant ? 1.0 : 0.0, "contribution", f3));
        featureImportance.put("timeOfDay", Map.of("weight", 0.10, "rawScore", timeScore, "contribution", f4));
        featureImportance.put("isHighRiskCategory", Map.of("weight", 0.15, "rawScore", isHighRiskCategory ? 1.0 : 0.0, "contribution", f5));
        featureImportance.put("accountAge", Map.of("weight", 0.10, "rawScore", ageScore, "contribution", f6));
        featureImportance.put("transactionCount", Map.of("weight", 0.10, "rawScore", countScore, "contribution", f7));
        result.getOutputData().put("featureImportance", featureImportance);
        return result;
    }

    private static double toDouble(Object obj, double defaultVal) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        return defaultVal;
    }
}
