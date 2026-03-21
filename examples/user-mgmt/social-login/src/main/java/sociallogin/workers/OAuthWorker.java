package sociallogin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
import java.util.UUID;

public class OAuthWorker implements Worker {
    @Override public String getTaskDefName() { return "slo_auth"; }
    @Override public TaskResult execute(Task task) {
        String provider = (String) task.getInputData().get("provider");
        System.out.println("  [auth] OAuth token validated with " + provider);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("providerUserId", "prov_" + UUID.randomUUID().toString().substring(0, 8));
        result.getOutputData().put("profile", Map.of("name", "Grace Hopper", "avatar", "https://avatars.example.com/grace.jpg"));
        return result;
    }
}
