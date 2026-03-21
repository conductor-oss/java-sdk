package featureflagrollout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FullRolloutWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_full_rollout"; }

    @Override
    public TaskResult execute(Task task) {
        String flagName = (String) task.getInputData().get("flagName");
        if (flagName == null) flagName = "unknown-flag";

        Object healthyObj = task.getInputData().get("healthy");
        boolean healthy = false;
        if (healthyObj instanceof Boolean) healthy = (Boolean) healthyObj;
        else if (healthyObj instanceof String) healthy = "true".equalsIgnoreCase((String) healthyObj);

        String action = healthy ? "activated" : "rolled back";
        System.out.println("  [rollout] Full rollout: " + action);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("active", healthy);
        result.getOutputData().put("flagName", flagName);
        return result;
    }
}
