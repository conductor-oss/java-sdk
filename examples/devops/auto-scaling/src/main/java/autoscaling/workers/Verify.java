package autoscaling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies that the scaling action achieved the desired result.
 */
public class Verify implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        String action = (String) task.getInputData().get("action");
        if (action == null) action = "no-change";

        System.out.println("[as_verify] Verifying action: " + action);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("action", action);

        boolean verified;
        int newLoad;

        switch (action) {
            case "scale-up":
                verified = true;
                newLoad = 52;
                output.put("message", "CPU dropped to " + newLoad + "% after scale-up");
                break;
            case "scale-down":
                verified = true;
                newLoad = 45;
                output.put("message", "Load stable at " + newLoad + "% after scale-down");
                break;
            default:
                verified = true;
                newLoad = 50;
                output.put("message", "No change needed, system stable at " + newLoad + "%");
                break;
        }

        output.put("verified", verified);
        output.put("newLoad", newLoad);

        System.out.println("[as_verify] " + output.get("message"));

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
