package releasemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rm_approve";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [approve] Release approved by release manager");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("approve", true);
        result.addOutputData("processed", true);
        return result;
    }
}
