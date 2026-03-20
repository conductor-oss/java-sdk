package loyaltyrewards.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckTierWorker implements Worker {
    @Override public String getTaskDefName() { return "lyr_check_tier"; }
    @Override public TaskResult execute(Task task) {
        int total = 1300;
        Object tp = task.getInputData().get("totalPoints");
        if (tp instanceof Number) total = ((Number) tp).intValue();
        String tier = total >= 2000 ? "Platinum" : total >= 1000 ? "Gold" : "Silver";
        System.out.println("  [tier] Customer tier: " + tier + " (" + total + " points)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("tier", tier);
        result.addOutputData("totalPoints", total);
        return result;
    }
}
