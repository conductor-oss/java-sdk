package fundraisingcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "frc_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Final report for campaign " + task.getInputData().get("campaignId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("campaign", Map.of("campaignId", task.getInputData().getOrDefault("campaignId","CMP-754"), "raised", task.getInputData().getOrDefault("raised",112000), "goalMet", true, "status", "COMPLETED")); return r;
    }
}
