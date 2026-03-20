package autoscaling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes the planned scaling action (scale-up, scale-down, or no-change).
 */
public class Execute implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_execute";
    }

    @Override
    public TaskResult execute(Task task) {
        String action = (String) task.getInputData().get("action");
        if (action == null) action = "no-change";

        System.out.println("[as_execute] Executing action: " + action);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("action", action);

        boolean scaled;
        switch (action) {
            case "scale-up":
                scaled = true;
                output.put("scaled", true);
                output.put("newInstanceCount", 5);
                output.put("message", "Scaled up to 5 replicas");
                break;
            case "scale-down":
                scaled = true;
                output.put("scaled", true);
                output.put("newInstanceCount", 2);
                output.put("message", "Scaled down to 2 replicas");
                break;
            default:
                scaled = false;
                output.put("scaled", false);
                output.put("newInstanceCount", 3);
                output.put("message", "No scaling action required");
                break;
        }

        output.put("executedAt", Instant.now().toString());

        System.out.println("[as_execute] " + output.get("message"));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
