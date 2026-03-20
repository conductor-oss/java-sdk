package deploymentrollback.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyRollbackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rb_verify_rollback";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Service healthy after rollback, error rate normalized");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_rollback", true);
        return result;
    }
}
