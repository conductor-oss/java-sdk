package wiretransfer.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class VerifySenderWorker implements Worker {
    @Override public String getTaskDefName() { return "wir_verify_sender"; }
    @Override public TaskResult execute(Task task) {
        Object amtObj = task.getInputData().get("amount");
        double amount = amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0;
        System.out.println("  [sender] Verifying account " + task.getInputData().get("senderAccount") + " — balance sufficient for $" + amount);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("verified", true); r.getOutputData().put("balance", 1500000);
        r.getOutputData().put("sufficientFunds", true); r.getOutputData().put("authorizationLevel", amount > 100000 ? "dual" : "single");
        return r;
    }
}
