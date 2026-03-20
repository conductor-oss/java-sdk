package premiumcalculation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pmc_finalize";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [finalize] Final premium: $1,530/year");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalPremium", 1530);
        result.getOutputData().put("breakdown", java.util.Map.of("base", 1800, "discounts", -270, "final", 1530));
        return result;
    }
}
