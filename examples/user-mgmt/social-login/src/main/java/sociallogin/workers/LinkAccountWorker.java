package sociallogin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.UUID;

public class LinkAccountWorker implements Worker {
    @Override public String getTaskDefName() { return "slo_link_account"; }
    @Override public TaskResult execute(Task task) {
        String provider = (String) task.getInputData().get("provider");
        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("  [link] Account linked/created -> " + userId + " via " + provider);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("userId", userId);
        result.getOutputData().put("isNewUser", false);
        result.getOutputData().put("linkedProviders", List.of(provider));
        return result;
    }
}
