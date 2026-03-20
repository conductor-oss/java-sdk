package virtualeconomy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class TransactionWorker implements Worker {
    @Override public String getTaskDefName() { return "vec_transaction"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [txn] " + task.getInputData().get("type") + " " + task.getInputData().get("amount") + " for player " + task.getInputData().get("playerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("transactionId", "VTXN-750"); r.addOutputData("transaction", Map.of("type", task.getInputData().getOrDefault("type","earn"), "amount", task.getInputData().getOrDefault("amount",100)));
        return r;
    }
}
