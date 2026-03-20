package featureflags.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckFlagWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_check_flag"; }

    @Override public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().getOrDefault("userId", "unknown");
        String featureName = (String) task.getInputData().getOrDefault("featureName", "unknown");
        boolean enabled = "new-checkout-ui".equals(featureName);
        System.out.println("  [check] Flag \"" + featureName + "\" for user " + userId + ": " + (enabled ? "enabled" : "disabled") + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("flagStatus", enabled ? "enabled" : "disabled");
        result.getOutputData().put("rolloutPercent", enabled ? 100 : 0);
        result.getOutputData().put("source", "launchdarkly");
        return result;
    }
}
