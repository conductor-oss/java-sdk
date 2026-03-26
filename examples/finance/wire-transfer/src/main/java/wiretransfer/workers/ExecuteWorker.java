package wiretransfer.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "wir_execute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [execute] Transferring $" + task.getInputData().get("amount") + " " + task.getInputData().get("currency") + " via Fedwire");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("transactionRef", "FW-2024-88201-X"); r.getOutputData().put("network", "Fedwire");
        r.getOutputData().put("settledAt", Instant.now().toString()); r.getOutputData().put("fee", 25.00);
        return r;
    }
}
