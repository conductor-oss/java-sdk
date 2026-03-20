package nonprofitdonation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReceiptWorker implements Worker {
    @Override public String getTaskDefName() { return "don_receipt"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [receipt] Tax receipt sent to " + task.getInputData().get("donorName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("receiptId", "RCT-751"); r.addOutputData("taxDeductible", true);
        return r;
    }
}
