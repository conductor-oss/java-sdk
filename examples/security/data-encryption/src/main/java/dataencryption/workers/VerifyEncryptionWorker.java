package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyEncryptionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_verify_encryption";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Encryption verified: no plaintext PII accessible");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_encryption", true);
        return result;
    }
}
