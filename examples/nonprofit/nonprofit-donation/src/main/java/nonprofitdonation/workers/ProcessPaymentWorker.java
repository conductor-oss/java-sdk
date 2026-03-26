package nonprofitdonation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ProcessPaymentWorker implements Worker {
    @Override public String getTaskDefName() { return "don_process_payment"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [payment] Processing $" + task.getInputData().get("amount") + " from " + task.getInputData().get("donorName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("transactionId", "TXN-D751"); r.addOutputData("processed", true);
        return r;
    }
}
