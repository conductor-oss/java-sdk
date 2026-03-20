package deploymentrollback.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RollbackDeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rb_rollback_deploy";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [rollback] Rolled back to previous stable version");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("rollback_deploy", true);
        result.addOutputData("processed", true);
        return result;
    }
}
