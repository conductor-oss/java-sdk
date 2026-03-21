package salvagerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CloseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slv_close";
    }

    @Override
    public TaskResult execute(Task task) {

        String claimId = (String) task.getInputData().get("claimId");
        System.out.printf("  [close] Claim %s closed%n", claimId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("closed", true);
        result.getOutputData().put("closedAt", "2024-03-20");
        return result;
    }
}
