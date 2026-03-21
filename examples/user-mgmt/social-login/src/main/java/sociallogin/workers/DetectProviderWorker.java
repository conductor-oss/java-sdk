package sociallogin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectProviderWorker implements Worker {
    @Override public String getTaskDefName() { return "slo_detect_provider"; }
    @Override public TaskResult execute(Task task) {
        String provider = (String) task.getInputData().get("provider");
        System.out.println("  [detect] OAuth provider detected: " + provider);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("providerName", provider);
        result.getOutputData().put("authEndpoint", "https://" + provider + ".example.com/oauth");
        result.getOutputData().put("supported", true);
        return result;
    }
}
