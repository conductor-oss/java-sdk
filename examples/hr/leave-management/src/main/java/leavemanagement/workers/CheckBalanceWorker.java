package leavemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckBalanceWorker implements Worker {
    @Override public String getTaskDefName() { return "lvm_check_balance"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [balance] " + task.getInputData().get("leaveType") +
                " balance: 15 days, requested: " + task.getInputData().get("requested"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("balance", 15);
        result.getOutputData().put("sufficient", true);
        return result;
    }
}
