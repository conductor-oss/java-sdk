package competitiveagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Select winner — takes the judge's judgment and all solutions,
 * produces a winner map and a ranked list of all solutions.
 */
public class SelectWinnerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_select_winner";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> judgment = (Map<String, Object>) task.getInputData().get("judgment");
        Map<String, Object> allSolutions = (Map<String, Object>) task.getInputData().get("allSolutions");

        String winnerId = judgment != null ? (String) judgment.get("winner") : "unknown";
        String winnerTitle = judgment != null ? (String) judgment.get("winnerTitle") : "Unknown";
        String reasoning = judgment != null ? (String) judgment.get("reasoning") : "";
        List<Map<String, Object>> scores = judgment != null
                ? (List<Map<String, Object>>) judgment.get("scores")
                : List.of();

        // Build the winner map from the winning solution
        Map<String, Object> winningSolution = null;
        if (allSolutions != null) {
            int solverIndex = switch (winnerId) {
                case "solver1" -> 1;
                case "solver2" -> 2;
                case "solver3" -> 3;
                default -> 0;
            };
            if (solverIndex > 0) {
                winningSolution = (Map<String, Object>) allSolutions.get("solution" + solverIndex);
            }
        }

        Map<String, Object> winner = new LinkedHashMap<>();
        winner.put("solverId", winnerId);
        winner.put("title", winnerTitle);
        winner.put("reasoning", reasoning);
        if (winningSolution != null) {
            winner.put("approach", winningSolution.get("approach"));
            winner.put("estimatedCost", winningSolution.get("estimatedCost"));
            winner.put("timeline", winningSolution.get("timeline"));
        }

        // Build rankings array from the scores (already sorted by judge)
        List<Map<String, Object>> rankings = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            Map<String, Object> scoreEntry = scores.get(i);
            String solverId = (String) scoreEntry.get("solverId");
            int solverIdx = switch (solverId) {
                case "solver1" -> 1;
                case "solver2" -> 2;
                case "solver3" -> 3;
                default -> 0;
            };
            Map<String, Object> solnData = allSolutions != null && solverIdx > 0
                    ? (Map<String, Object>) allSolutions.get("solution" + solverIdx) : null;

            Map<String, Object> ranking = new LinkedHashMap<>();
            ranking.put("rank", i + 1);
            ranking.put("solverId", solverId);
            ranking.put("title", solnData != null ? solnData.get("title") : "Unknown");
            ranking.put("totalScore", scoreEntry.get("totalScore"));
            rankings.add(ranking);
        }

        System.out.println("  [comp_select_winner] Selected winner: " + winnerTitle);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("winner", winner);
        result.getOutputData().put("rankings", rankings);
        return result;
    }
}
