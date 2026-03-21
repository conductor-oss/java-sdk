package secretrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class GenerateSecretWorker implements Worker {
    @Override public String getTaskDefName() { return "sr_generate_secret"; }

    @Override public TaskResult execute(Task task) {
        String secretName = (String) task.getInputData().getOrDefault("secretName", "unknown");
        String algorithm = (String) task.getInputData().getOrDefault("algorithm", "AES-256");
        System.out.println("  [generate] Generating new secret for \"" + secretName + "\"...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("secretId", "sec-rot-20240301-001");
        result.getOutputData().put("algorithm", algorithm);
        result.getOutputData().put("expiresAt", "2024-06-01T00:00:00Z");
        result.getOutputData().put("generatedAt", Instant.now().toString());
        return result;
    }
}
