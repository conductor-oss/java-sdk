package webhooksecurity.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Computes an HMAC signature for a webhook payload.
 * Input: payload, secret
 * Output: computedSignature, algorithm
 *
 * Uses a deterministic fixed value for the computed signature.
 */
public class ComputeHmacWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ws_compute_hmac";
    }

    @Override
    public TaskResult execute(Task task) {
        String payload = (String) task.getInputData().get("payload");
        String secret = (String) task.getInputData().get("secret");

        if (payload == null) {
            payload = "";
        }
        if (secret == null) {
            secret = "";
        }

        System.out.println("  [ws_compute_hmac] Computing HMAC for payload length: " + payload.length());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("computedSignature", "hmac_sha256_fixedvalue");
        result.getOutputData().put("algorithm", "sha256");
        return result;
    }
}
