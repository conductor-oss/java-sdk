package digitalassetmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Distributes the asset to configured channels (website, mobile, portal).
 * Input: assetId, assetUrl, projectId
 * Output: distributedTo, cdnUrls
 */
public class DistributeAssetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dam_distribute_asset";
    }

    @Override
    public TaskResult execute(Task task) {
        String projectId = (String) task.getInputData().get("projectId");
        if (projectId == null) projectId = "unknown";

        System.out.println("  [distribute] Distributing asset to project " + projectId + " channels");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("distributedTo", List.of("website", "mobile_app", "marketing_portal"));
        result.getOutputData().put("cdnUrls", List.of(
                "https://cdn.example.com/dam/521/web.jpg",
                "https://cdn.example.com/dam/521/mobile.jpg"));
        return result;
    }
}
