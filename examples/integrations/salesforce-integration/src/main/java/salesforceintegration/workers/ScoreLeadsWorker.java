package salesforceintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Scores leads using a model.
 * Input: leads, model
 * Output: scoredLeads, scoredCount
 */
public class ScoreLeadsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfc_score_leads";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> leads =
                (java.util.List<java.util.Map<String, Object>>) task.getInputData().getOrDefault("leads", java.util.List.of());
        String model = (String) task.getInputData().get("model");

        java.util.List<java.util.Map<String, Object>> scored = new java.util.ArrayList<>();
        int score = 85;
        for (java.util.Map<String, Object> lead : leads) {
            java.util.Map<String, Object> s = new java.util.HashMap<>(lead);
            s.put("score", score);
            scored.add(s);
            score -= 10;
        }
        System.out.println("  [score] Scored " + scored.size() + " leads using model " + model);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scoredLeads", scored);
        result.getOutputData().put("scoredCount", scored.size());
        return result;
    }
}
