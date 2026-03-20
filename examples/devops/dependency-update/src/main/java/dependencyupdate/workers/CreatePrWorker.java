package dependencyupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CreatePrWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "du_create_pr";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [pr] Pull request created with dependency diff");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("create_pr", true);
        return result;
    }
}
