package salvagerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SettleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slv_settle";
    }

    @Override
    public TaskResult execute(Task task) {

        String claimId = (String) task.getInputData().get("claimId");
        System.out.printf("  [settle] Claim %s recovered%n", claimId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recoveryAmount", 5100);
        result.getOutputData().put("netRecovery", 4800);
        return result;
    }
}
