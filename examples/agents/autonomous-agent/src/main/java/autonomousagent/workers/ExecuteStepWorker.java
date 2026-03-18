package autonomousagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Executes a single step from the plan based on the current iteration.
 * Input:  {plan, iteration}
 * Output: {result, stepNumber, status}
 */
public class ExecuteStepWorker implements Worker {

    private static final String[] STEP_RESULTS = {
        "Metrics pipeline deployed: 15 exporters active, scrape interval 15s, retention 30d",
        "Dashboard created: 12 panels covering CPU, memory, disk, network, and application metrics",
        "Alerts configured: 8 rules for critical thresholds, Slack + PagerDuty notifications enabled"
    };

    @Override
    public String getTaskDefName() {
        return "aa_execute_step";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> plan = (List<String>) task.getInputData().get("plan");
        if (plan == null) {
            plan = List.of();
        }

        int iteration = toInt(task.getInputData().get("iteration"), 1);

        int index = (iteration - 1) % STEP_RESULTS.length;
        if (index < 0) index = 0;
        String stepResult = STEP_RESULTS[index];

        System.out.println("  [aa_execute_step] Iteration " + iteration + ": " + stepResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", stepResult);
        result.getOutputData().put("stepNumber", iteration);
        result.getOutputData().put("status", "complete");
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
