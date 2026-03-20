package certificatemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RenewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_renew";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [renew] 5 certificates renewed successfully");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("renew", true);
        result.addOutputData("processed", true);
        return result;
    }
}
