package dripcampaign.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class SendSeriesWorker implements Worker {
    @Override public String getTaskDefName() { return "drp_send_series"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [send] Drip series: 5 emails sent to " + task.getInputData().get("email"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("emailsSent", 5);
        result.getOutputData().put("series", List.of("Welcome", "Value Prop", "Case Study", "Demo Invite", "Special Offer"));
        return result;
    }
}
