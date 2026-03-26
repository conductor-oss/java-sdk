package premiumcalculation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CalculateBaseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pmc_calculate_base";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [base] Base premium calculated: $1,800/year");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("basePremium", 1800);
        return result;
    }
}
