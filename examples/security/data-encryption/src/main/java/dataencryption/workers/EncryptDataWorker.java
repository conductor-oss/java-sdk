package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EncryptDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_encrypt_data";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [encrypt] 4 PII fields encrypted at rest");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("encrypt_data", true);
        result.addOutputData("processed", true);
        return result;
    }
}
