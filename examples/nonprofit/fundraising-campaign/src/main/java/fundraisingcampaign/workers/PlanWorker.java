package fundraisingcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class PlanWorker implements Worker {
    @Override public String getTaskDefName() { return "frc_plan"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [plan] Planning campaign: " + task.getInputData().get("campaignName") + ", goal: $" + task.getInputData().get("goalAmount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("campaignId", "CMP-754"); r.addOutputData("channels", List.of("email","social","direct-mail")); return r;
    }
}
