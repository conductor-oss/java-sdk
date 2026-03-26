package loyaltyrewards.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "lyr_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Loyalty updated: earned=" + task.getInputData().get("earned") + ", redeemed=" + task.getInputData().get("redeemed"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("loyalty", Map.of(
            "customerId", task.getInputData().getOrDefault("customerId", "CUST-42"),
            "earned", task.getInputData().getOrDefault("earned", 0),
            "redeemed", task.getInputData().getOrDefault("redeemed", 0),
            "status", "UPDATED"));
        return result;
    }
}
