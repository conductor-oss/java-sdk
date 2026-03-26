package inapppurchase.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReceiptWorker implements Worker {
    @Override public String getTaskDefName() { return "iap_receipt"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [receipt] Receipt generated for transaction " + task.getInputData().get("transactionId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("receipt", Map.of("transactionId", task.getInputData().getOrDefault("transactionId","TXN-7441"), "playerId", task.getInputData().getOrDefault("playerId","P-042"), "status", "COMPLETE"));
        return r;
    }
}
