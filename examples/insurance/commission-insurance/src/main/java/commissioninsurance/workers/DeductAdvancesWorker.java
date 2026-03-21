package commissioninsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeductAdvancesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cin_deduct_advances";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [deduct] Advance deduction applied");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("netCommission", 430.0);
        result.getOutputData().put("advanceDeducted", 50);
        return result;
    }
}
