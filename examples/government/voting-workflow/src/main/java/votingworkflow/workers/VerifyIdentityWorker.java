package votingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyIdentityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vtw_verify_identity";
    }

    @Override
    public TaskResult execute(Task task) {
        String voterId = (String) task.getInputData().get("voterId");
        System.out.printf("  [verify] Identity verified for voter %s%n", voterId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        return result;
    }
}
