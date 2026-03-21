package atleastonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AloReceiveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "alo_receive";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [receive] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("receiptHandle", "rcpt-" + Long.toString(System.currentTimeMillis(), 36));
        result.getOutputData().put("deliveryCount", 1);
        result.getOutputData().put("visibilityTimeout", 30);
        return result;
    }
}