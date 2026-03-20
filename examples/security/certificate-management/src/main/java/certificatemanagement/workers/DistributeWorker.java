package certificatemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DistributeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_distribute";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [distribute] Renewed certificates distributed to 12 endpoints");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("distribute", true);
        return result;
    }
}
