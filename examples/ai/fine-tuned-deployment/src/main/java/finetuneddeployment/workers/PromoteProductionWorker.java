package finetuneddeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Promotes the model from staging to production.
 */
public class PromoteProductionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ftd_promote_production";
    }

    @Override
    public TaskResult execute(Task task) {
        String modelId = (String) task.getInputData().get("modelId");

        System.out.println("  [promote] Promoting " + modelId + " to production...");

        String prodEndpoint = "https://api.models.internal/" + modelId;
        System.out.println("  [promote] Production endpoint: " + prodEndpoint);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("prodEndpoint", prodEndpoint);
        result.getOutputData().put("status", "live");
        result.getOutputData().put("trafficPercent", 100);
        return result;
    }
}
