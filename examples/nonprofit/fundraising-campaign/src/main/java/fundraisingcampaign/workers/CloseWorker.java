package fundraisingcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CloseWorker implements Worker {
    @Override public String getTaskDefName() { return "frc_close"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [close] Campaign " + task.getInputData().get("campaignId") + " closed on " + task.getInputData().get("endDate"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("closed", true); r.addOutputData("closedAt", task.getInputData().get("endDate")); return r;
    }
}
