package ragembeddingselection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that selects the best embedding model based on evaluation rankings.
 * Returns the best model, its score, and a recommendation string.
 */
public class SelectBestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "es_select_best";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> rankings = (List<Map<String, Object>>) task.getInputData().get("rankings");
        Map<String, Object> evaluation = (Map<String, Object>) task.getInputData().get("evaluation");

        Map<String, Object> top = rankings.get(0);
        String bestModel = (String) top.get("model");
        double bestScore = EvaluateEmbeddingsWorker.toDouble(top.get("compositeScore"));

        String bestQuality = (String) evaluation.get("bestQuality");
        String fastestLatency = (String) evaluation.get("fastestLatency");
        String lowestCost = (String) evaluation.get("lowestCost");

        String recommendation = "Recommended: " + bestModel
                + " (composite=" + bestScore + ")."
                + " Best quality: " + bestQuality + "."
                + " Fastest: " + fastestLatency + "."
                + " Cheapest: " + lowestCost + ".";

        System.out.println("  [select] " + recommendation);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bestModel", bestModel);
        result.getOutputData().put("bestScore", bestScore);
        result.getOutputData().put("recommendation", recommendation);
        return result;
    }
}
