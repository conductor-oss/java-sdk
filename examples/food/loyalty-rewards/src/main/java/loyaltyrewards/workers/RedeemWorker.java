package loyaltyrewards.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RedeemWorker implements Worker {
    @Override public String getTaskDefName() { return "lyr_redeem"; }
    @Override public TaskResult execute(Task task) {
        int pts = 0;
        Object rp = task.getInputData().get("redeemPoints");
        if (rp instanceof Number) pts = ((Number) rp).intValue();
        System.out.println("  [redeem] Redeeming " + pts + " points (tier: " + task.getInputData().get("tier") + ")");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("redeemed", pts);
        result.addOutputData("discount", String.format("%.2f", pts * 0.01));
        result.addOutputData("success", true);
        return result;
    }
}
