package accessreview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EnforceDecisionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ar_enforce_decisions";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [enforce] 5 access grants revoked across 3 systems");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("enforce_decisions", true);
        return result;
    }
}
