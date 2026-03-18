package agenticloop.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Executes the action described by the plan and returns a result.
 * Results are deterministically mapped from plan strings.
 * Input:  {plan, iteration}
 * Output: {result, success:true}
 */
public class ActWorker implements Worker {

    private static final Map<String, String> RESULTS = Map.of(
        "Research and gather information on the topic",
            "Collected 15 relevant sources including academic papers, industry reports, and expert interviews",
        "Analyze gathered data and identify key patterns",
            "Identified 5 key patterns: consistency, partition tolerance, replication strategies, consensus protocols, and failure recovery",
        "Synthesize findings into actionable recommendations",
            "Produced a prioritized list of 8 recommendations with implementation guidelines and trade-off analysis"
    );

    private static final String DEFAULT_RESULT = "Completed action step successfully";

    @Override
    public String getTaskDefName() {
        return "al_act";
    }

    @Override
    public TaskResult execute(Task task) {
        String plan = (String) task.getInputData().get("plan");
        if (plan == null || plan.isBlank()) {
            plan = "";
        }

        int iteration = toInt(task.getInputData().get("iteration"), 1);

        String actionResult = RESULTS.getOrDefault(plan, DEFAULT_RESULT);

        System.out.println("  [al_act] Iteration " + iteration + ": " + actionResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", actionResult);
        result.getOutputData().put("success", true);
        return result;
    }

    private int toInt(Object value, int defaultVal) {
        if (value == null) return defaultVal;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
