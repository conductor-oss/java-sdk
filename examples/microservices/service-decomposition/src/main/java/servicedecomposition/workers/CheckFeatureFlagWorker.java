package servicedecomposition.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks feature flag to determine routing target.
 * Input: feature
 * Output: target, percentage
 *
 * Deterministic: always routes to "monolith" with 100% traffic.
 */
public class CheckFeatureFlagWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sd_check_feature_flag";
    }

    @Override
    public TaskResult execute(Task task) {
        String feature = (String) task.getInputData().get("feature");
        if (feature == null) feature = "unknown";

        System.out.println("  [sd_check_feature_flag] Feature " + feature + ": routing to monolith");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("target", "monolith");
        result.getOutputData().put("percentage", 100);
        return result;
    }
}
