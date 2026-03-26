package ragqualitygates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that checks the relevance of retrieved documents.
 * Computes the average score across all documents and compares
 * against a threshold of 0.7. Returns relevanceScore, threshold,
 * and decision (pass/fail).
 */
public class CheckRelevanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qg_check_relevance";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> documents = (List<Map<String, Object>>) task.getInputData().get("documents");
        if (documents == null) {
            documents = List.of();
        }

        double sum = 0.0;
        for (Map<String, Object> doc : documents) {
            Object scoreObj = doc.get("score");
            if (scoreObj instanceof Number) {
                sum += ((Number) scoreObj).doubleValue();
            }
        }
        double relevanceScore = documents.isEmpty() ? 0.0 : sum / documents.size();
        double threshold = 0.7;
        String decision = relevanceScore >= threshold ? "pass" : "fail";

        System.out.println("  [check_relevance] Average relevance: " + String.format("%.2f", relevanceScore)
                + " (threshold: " + threshold + ") -> " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("relevanceScore", relevanceScore);
        result.getOutputData().put("threshold", threshold);
        result.getOutputData().put("decision", decision);
        return result;
    }
}
