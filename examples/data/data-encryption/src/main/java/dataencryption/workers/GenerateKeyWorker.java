package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Generates an encryption key.
 * Input: algorithm (string)
 * Output: keyId (string), algorithm (string), createdAt (string)
 */
public class GenerateKeyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dn_generate_key";
    }

    @Override
    public TaskResult execute(Task task) {
        String algorithm = (String) task.getInputData().getOrDefault("algorithm", "AES-256");
        String keyId = "KEY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [keygen] Generated " + algorithm + " key: " + keyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("keyId", keyId);
        result.getOutputData().put("algorithm", algorithm);
        result.getOutputData().put("createdAt", Instant.now().toString());
        return result;
    }
}
