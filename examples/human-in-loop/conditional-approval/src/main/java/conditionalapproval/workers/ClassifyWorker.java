package conditionalapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for car_classify — classifies a request amount into a tier.
 *
 * Tier classification:
 * - amount < 1000:  "low"   (manager approval only)
 * - 1000 <= amount < 10000: "medium" (manager + director)
 * - amount >= 10000: "high" (manager + director + VP)
 */
public class ClassifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "car_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        double amount = 0;
        Object amountInput = task.getInputData().get("amount");
        if (amountInput instanceof Number) {
            amount = ((Number) amountInput).doubleValue();
        }

        String tier;
        if (amount >= 10000) {
            tier = "high";
        } else if (amount >= 1000) {
            tier = "medium";
        } else {
            tier = "low";
        }

        System.out.println("  [car_classify] $" + amount + " -> " + tier + " tier");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tier", tier);
        return result;
    }
}
