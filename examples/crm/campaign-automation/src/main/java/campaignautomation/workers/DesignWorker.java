package campaignautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class DesignWorker implements Worker {
    @Override public String getTaskDefName() { return "cpa_design"; }

    @Override
    public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("name");
        String campaignId = "CMP-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        System.out.println("  [design] Campaign \"" + name + "\" designed -> " + campaignId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("campaignId", campaignId);
        result.getOutputData().put("type", task.getInputData().get("type"));
        result.getOutputData().put("creativeAssets", 5);
        result.getOutputData().put("channels", List.of("email", "social", "display"));
        return result;
    }
}
