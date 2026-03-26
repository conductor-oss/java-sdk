package digitalassetmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Auto-tags an asset using AI-based analysis.
 * Input: assetId, assetType, storagePath
 * Output: tags, aiConfidence, colorPalette
 */
public class AutoTagWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dam_auto_tag";
    }

    @Override
    public TaskResult execute(Task task) {
        String assetType = (String) task.getInputData().get("assetType");
        if (assetType == null) assetType = "unknown";

        System.out.println("  [tag] Auto-tagging " + assetType + " asset");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tags", List.of("product_photo", "lifestyle", "outdoor", "summer_2026"));
        result.getOutputData().put("aiConfidence", 0.91);
        result.getOutputData().put("colorPalette", List.of("#2C5F2D", "#97BC62", "#F5F5DC"));
        return result;
    }
}
