package inapppurchase.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ChargeWorker implements Worker {
    @Override public String getTaskDefName() { return "iap_charge"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [charge] Charging $" + task.getInputData().get("price") + " to player " + task.getInputData().get("playerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("transactionId", "TXN-7441"); r.addOutputData("charged", true); r.addOutputData("amount", task.getInputData().get("price"));
        return r;
    }
}
