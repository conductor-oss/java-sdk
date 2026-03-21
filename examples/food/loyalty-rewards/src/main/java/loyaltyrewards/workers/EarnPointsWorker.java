package loyaltyrewards.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EarnPointsWorker implements Worker {
    @Override public String getTaskDefName() { return "lyr_earn_points"; }
    @Override public TaskResult execute(Task task) {
        int earned = 50;
        Object orderTotal = task.getInputData().get("orderTotal");
        if (orderTotal instanceof Number) earned = ((Number) orderTotal).intValue();
        System.out.println("  [earn] Customer " + task.getInputData().get("customerId") + " earned " + earned + " points");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("earned", earned);
        result.addOutputData("totalPoints", 1250 + earned);
        return result;
    }
}
