package leadscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;
import java.util.Set;

/**
 * Computes lead score using weighted scoring algorithm.
 * Real multi-factor scoring based on company size, industry, engagement, and behavior.
 */
public class ScoreWorker implements Worker {
    private static final Map<String, Integer> COMPANY_SIZE_SCORES = Map.of(
            "enterprise", 30, "mid_market", 25, "small_business", 15, "startup", 10);
    private static final Set<String> HIGH_VALUE_INDUSTRIES = Set.of(
            "technology", "finance", "healthcare", "saas", "fintech");

    @Override public String getTaskDefName() { return "ls_score"; }

    @Override @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object signalsObj = task.getInputData().get("signals");
        Map<String, Object> signals = signalsObj instanceof Map ? (Map<String, Object>) signalsObj : Map.of();

        String companySize = (String) signals.getOrDefault("companySize", "unknown");
        String industry = (String) signals.getOrDefault("industry", "unknown");
        int pageViews = signals.get("pageViews") instanceof Number ? ((Number) signals.get("pageViews")).intValue() : 0;
        int emailOpens = signals.get("emailOpens") instanceof Number ? ((Number) signals.get("emailOpens")).intValue() : 0;
        boolean demoRequested = Boolean.TRUE.equals(signals.get("demoRequested"));

        // Weighted scoring
        int companySizeScore = COMPANY_SIZE_SCORES.getOrDefault(companySize.toLowerCase(), 5);
        int industryScore = HIGH_VALUE_INDUSTRIES.contains(industry.toLowerCase()) ? 20 : 10;
        int engagementScore = Math.min(25, pageViews * 1 + emailOpens * 3);
        int behaviorScore = demoRequested ? 25 : 0;

        int totalScore = companySizeScore + industryScore + engagementScore + behaviorScore;

        System.out.println("  [score] Lead score: " + totalScore + " (company=" + companySizeScore
                + ", industry=" + industryScore + ", engagement=" + engagementScore
                + ", behavior=" + behaviorScore + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalScore", totalScore);
        result.getOutputData().put("companySizeScore", companySizeScore);
        result.getOutputData().put("industryScore", industryScore);
        result.getOutputData().put("engagementScore", engagementScore);
        result.getOutputData().put("behaviorScore", behaviorScore);
        return result;
    }
}
