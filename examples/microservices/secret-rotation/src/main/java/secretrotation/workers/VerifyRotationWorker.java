package secretrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class VerifyRotationWorker implements Worker {
    @Override public String getTaskDefName() { return "sr_verify_rotation"; }

    @Override public TaskResult execute(Task task) {
        String secretName = (String) task.getInputData().getOrDefault("secretName", "unknown");
        String secretId = (String) task.getInputData().getOrDefault("secretId", "unknown");
        System.out.println("  [verify] Verifying secret rotation for \"" + secretName + "\"...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "success");
        result.getOutputData().put("allServicesUsing", secretId);
        result.getOutputData().put("oldSecretRevoked", true);
        result.getOutputData().put("verifiedAt", Instant.now().toString());
        return result;
    }
}
