package nonprofitdonation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReceiveWorker implements Worker {
    @Override public String getTaskDefName() { return "don_receive"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [receive] Donation received from " + task.getInputData().get("donorName") + ": $" + task.getInputData().get("amount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("received", true); r.addOutputData("donationId", "DON-751");
        return r;
    }
}
