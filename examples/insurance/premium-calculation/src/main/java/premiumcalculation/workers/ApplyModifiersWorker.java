package premiumcalculation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplyModifiersWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pmc_apply_modifiers";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [modifiers] Good driver discount (-10%%), multi-policy (-5%%)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("adjustedPremium", 1530);
        result.getOutputData().put("discounts", java.util.List.of("good-driver", "multi-policy"));
        return result;
    }
}
