package competitiveagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Judge agent — evaluates all three solver solutions, scores each on cost,
 * innovation, and risk, then ranks them to determine a winner.
 */
public class JudgeAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_judge_agent";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> solution1 = (Map<String, Object>) task.getInputData().get("solution1");
        Map<String, Object> solution2 = (Map<String, Object>) task.getInputData().get("solution2");
        Map<String, Object> solution3 = (Map<String, Object>) task.getInputData().get("solution3");

        System.out.println("  [comp_judge_agent] Evaluating 3 competing solutions...");

        Map<String, Object> scores1 = scoreSolution(solution1, "solver1");
        Map<String, Object> scores2 = scoreSolution(solution2, "solver2");
        Map<String, Object> scores3 = scoreSolution(solution3, "solver3");

        List<Map<String, Object>> allScores = new ArrayList<>(List.of(scores1, scores2, scores3));
        allScores.sort(Comparator.comparingInt(
                (Map<String, Object> s) -> (int) s.get("totalScore")).reversed());

        String winnerId = (String) allScores.get(0).get("solverId");
        Map<String, Object> winningSolution = switch (winnerId) {
            case "solver1" -> solution1;
            case "solver2" -> solution2;
            case "solver3" -> solution3;
            default -> Map.of();
        };
        String winnerTitle = winningSolution != null
                ? (String) winningSolution.get("title") : "Unknown";

        String reasoning = String.format(
                "%s wins with a total score of %d. Cost efficiency: %d/10, Innovation: %d/10, Risk management: %d/10.",
                winnerTitle,
                (int) allScores.get(0).get("totalScore"),
                (int) allScores.get(0).get("costScore"),
                (int) allScores.get(0).get("innovationScore"),
                (int) allScores.get(0).get("riskScore"));

        System.out.println("  [comp_judge_agent] Winner: " + winnerTitle);

        Map<String, Object> judgment = new LinkedHashMap<>();
        judgment.put("winner", winnerId);
        judgment.put("winnerTitle", winnerTitle);
        judgment.put("scores", allScores);
        judgment.put("reasoning", reasoning);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("judgment", judgment);
        return result;
    }

    /**
     * Scores a solution on costScore, innovationScore, and riskScore,
     * then computes a totalScore as their sum.
     */
    Map<String, Object> scoreSolution(Map<String, Object> solution, String solverId) {
        int costScore = 5;
        int innovationScore = 5;
        int riskScore = 5;

        if (solution != null) {
            costScore = costScoreFromEstimate((String) solution.get("estimatedCost"));
            Object innovObj = solution.get("innovationScore");
            innovationScore = innovObj instanceof Number ? ((Number) innovObj).intValue() : 5;
            riskScore = riskScoreFromLevel((String) solution.get("riskLevel"));
        }

        int totalScore = costScore + innovationScore + riskScore;

        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("solverId", solverId);
        scores.put("costScore", costScore);
        scores.put("innovationScore", innovationScore);
        scores.put("riskScore", riskScore);
        scores.put("totalScore", totalScore);
        return scores;
    }

    /**
     * Derives a cost score (1-10) from an estimated cost string.
     * Lower cost yields higher score.
     */
    static int costScoreFromEstimate(String estimatedCost) {
        if (estimatedCost == null) return 5;
        String cleaned = estimatedCost.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return 5;
        int costK = Integer.parseInt(cleaned);
        if (costK <= 50) return 9;
        if (costK <= 80) return 7;
        if (costK <= 100) return 6;
        if (costK <= 130) return 4;
        return 3;
    }

    /**
     * Derives a risk score (1-10) from a risk level label.
     * Lower risk yields higher score.
     */
    static int riskScoreFromLevel(String riskLevel) {
        if (riskLevel == null) return 5;
        return switch (riskLevel.toLowerCase()) {
            case "very-low" -> 10;
            case "low" -> 8;
            case "medium" -> 6;
            case "medium-high" -> 4;
            case "high" -> 2;
            default -> 5;
        };
    }
}
