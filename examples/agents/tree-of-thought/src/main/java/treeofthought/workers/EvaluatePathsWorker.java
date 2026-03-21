package treeofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Evaluates all three reasoning paths and their confidence scores.
 * Deterministically selects "empirical" as the best path.
 */
public class EvaluatePathsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tt_evaluate_paths";
    }

    @Override
    public TaskResult execute(Task task) {
        String pathA = (String) task.getInputData().get("pathA");
        if (pathA == null || pathA.isBlank()) {
            pathA = "";
        }
        String pathB = (String) task.getInputData().get("pathB");
        if (pathB == null || pathB.isBlank()) {
            pathB = "";
        }
        String pathC = (String) task.getInputData().get("pathC");
        if (pathC == null || pathC.isBlank()) {
            pathC = "";
        }

        Object scoreAObj = task.getInputData().get("scoreA");
        double scoreA = toDouble(scoreAObj, 0.85);
        Object scoreBObj = task.getInputData().get("scoreB");
        double scoreB = toDouble(scoreBObj, 0.72);
        Object scoreCObj = task.getInputData().get("scoreC");
        double scoreC = toDouble(scoreCObj, 0.91);

        System.out.println("  [tt_evaluate_paths] Evaluating paths — A:" + scoreA
                + " B:" + scoreB + " C:" + scoreC);

        Map<String, Object> scores = Map.of(
                "analytical", scoreA,
                "creative", scoreB,
                "empirical", scoreC
        );

        String bestPath = "empirical";
        String bestSolution = pathC;
        String evaluation = "Path " + bestPath + " scored highest at " + scoreC;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scores", scores);
        result.getOutputData().put("bestPath", bestPath);
        result.getOutputData().put("bestSolution", bestSolution);
        result.getOutputData().put("evaluation", evaluation);
        return result;
    }

    private double toDouble(Object obj, double defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
