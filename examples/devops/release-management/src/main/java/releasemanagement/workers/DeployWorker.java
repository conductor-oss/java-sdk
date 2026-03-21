package releasemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rm_deploy";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [deploy] Version deployed to production");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("deploy", true);
        result.addOutputData("processed", true);
        return result;
    }
}
