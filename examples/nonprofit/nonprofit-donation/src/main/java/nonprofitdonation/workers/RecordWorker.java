package nonprofitdonation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class RecordWorker implements Worker {
    @Override public String getTaskDefName() { return "don_record"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [record] Donation recorded: " + task.getInputData().get("transactionId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("donation", Map.of("donor", task.getInputData().getOrDefault("donorName","Jane Smith"), "amount", task.getInputData().getOrDefault("amount",250), "transactionId", task.getInputData().getOrDefault("transactionId","TXN-D751"), "status", "COMPLETED"));
        return r;
    }
}
