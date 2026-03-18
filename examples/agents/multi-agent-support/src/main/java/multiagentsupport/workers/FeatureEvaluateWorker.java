package multiagentsupport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evaluates a feature request and provides priority, ETA, and roadmap tracking.
 */
public class FeatureEvaluateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_feature_evaluate";
    }

    @Override
    public TaskResult execute(Task task) {
        String subject = (String) task.getInputData().get("subject");
        String description = (String) task.getInputData().get("description");
        String customerTier = (String) task.getInputData().get("customerTier");

        if (subject == null) subject = "";
        if (description == null) description = "";
        if (customerTier == null) customerTier = "standard";

        System.out.println("  [cs_feature_evaluate] Evaluating feature request: " + subject);

        String response = "Thank you for your feature request. We have evaluated your suggestion "
                + "and it aligns with our product roadmap. Your request has been prioritized "
                + "and assigned to our development team. We will notify you when the feature "
                + "is available for beta testing.";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", response);
        result.getOutputData().put("priority", "high");
        result.getOutputData().put("eta", "Q2 2026");
        result.getOutputData().put("roadmapId", "FR-4521");
        return result;
    }
}
