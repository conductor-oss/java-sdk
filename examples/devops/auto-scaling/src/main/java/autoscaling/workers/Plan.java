package autoscaling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plans the scaling action based on current load analysis.
 */
public class Plan implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        int currentLoad = toInt(task.getInputData().get("currentLoad"), 50);

        System.out.println("[as_plan] Planning scaling for load: " + currentLoad + "%");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("currentLoad", currentLoad);

        String action;
        int from = 3;
        int to;

        if (currentLoad >= 80) {
            action = "scale-up";
            to = from + 2;
        } else if (currentLoad <= 30) {
            action = "scale-down";
            to = Math.max(1, from - 1);
        } else {
            action = "no-change";
            to = from;
        }

        output.put("action", action);
        output.put("from", from);
        output.put("to", to);
        output.put("reason", action.equals("scale-up")
                ? "Load " + currentLoad + "% exceeds threshold"
                : action.equals("scale-down")
                        ? "Load " + currentLoad + "% below minimum threshold"
                        : "Load " + currentLoad + "% within acceptable range");

        System.out.println("[as_plan] Decision: " + action + " (" + from + " -> " + to + ")");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }
}
