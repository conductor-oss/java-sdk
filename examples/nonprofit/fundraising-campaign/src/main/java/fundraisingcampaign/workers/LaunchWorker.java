package fundraisingcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class LaunchWorker implements Worker {
    @Override public String getTaskDefName() { return "frc_launch"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [launch] Campaign " + task.getInputData().get("campaignId") + " launched!");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("launched", true); r.addOutputData("launchDate", "2026-03-08"); return r;
    }
}
