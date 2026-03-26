package disasterrecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FailoverDbWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dr_failover_db";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [failover] DB promoted in us-west-2");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("failedOver", true);
        return result;
    }
}
