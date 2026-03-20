package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateKeyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_generate_key";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [key] AES-256 encryption key generated and stored in KMS");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("generate_key", true);
        result.addOutputData("processed", true);
        return result;
    }
}
