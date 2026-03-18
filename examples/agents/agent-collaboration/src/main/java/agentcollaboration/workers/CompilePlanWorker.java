package agentcollaboration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compile-plan agent — assembles insights, strategy, action items, and timeline
 * into a single consolidated plan summary.
 */
public class CompilePlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_compile_plan";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [ac_compile_plan] Compiling final plan...");

        List<Map<String, Object>> insights =
                (List<Map<String, Object>>) task.getInputData().get("insights");
        Map<String, Object> strategy =
                (Map<String, Object>) task.getInputData().get("strategy");
        List<Map<String, Object>> actionItems =
                (List<Map<String, Object>>) task.getInputData().get("actionItems");
        Map<String, Object> timeline =
                (Map<String, Object>) task.getInputData().get("timeline");

        int insightsUsed = (insights != null) ? insights.size() : 0;

        List<String> strategyPillars = List.of();
        String strategyName = "unknown";
        if (strategy != null) {
            strategyName = (String) strategy.getOrDefault("name", "unknown");
            Object pillarsObj = strategy.get("pillars");
            if (pillarsObj instanceof List<?>) {
                strategyPillars = ((List<?>) pillarsObj).stream()
                        .map(Object::toString)
                        .toList();
            }
        }

        int actionItemCount = (actionItems != null) ? actionItems.size() : 0;

        int totalWeeks = 0;
        if (timeline != null) {
            Object tw = timeline.get("totalWeeks");
            if (tw instanceof Number) {
                totalWeeks = ((Number) tw).intValue();
            }
        }

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("title", "Stabilize & Retain — Execution Plan");
        plan.put("insightsUsed", insightsUsed);
        plan.put("strategyPillars", strategyPillars.size());
        plan.put("actionItems", actionItemCount);
        plan.put("timeline", totalWeeks + " weeks");
        plan.put("status", "ready_for_review");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("plan", plan);
        return result;
    }
}
