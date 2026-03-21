package threeagentpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Reviewer agent — evaluates the draft for accuracy, clarity, and audience fit.
 */
public class ReviewerAgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "thr_reviewer_agent";
    }

    @Override
    public TaskResult execute(Task task) {
        String draft = (String) task.getInputData().get("draft");
        String audience = (String) task.getInputData().get("audience");
        if (audience == null || audience.isBlank()) {
            audience = "general readers";
        }

        System.out.println("  [reviewer-agent] Reviewing draft for audience: " + audience);

        List<String> suggestions = List.of(
                "Consider adding a concrete case study to strengthen the narrative",
                "The statistics section could benefit from year-over-year comparisons",
                "Add a brief executive summary at the beginning for busy readers"
        );

        Map<String, Object> review = Map.of(
                "score", 8.5,
                "maxScore", 10,
                "accuracy", "High — all facts are consistent with provided research",
                "clarity", "Good — well-structured with logical flow",
                "audienceFit", "Strong — appropriate tone and depth for " + audience,
                "suggestions", suggestions,
                "verdict", "APPROVED_WITH_MINOR_REVISIONS"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("review", review);
        result.getOutputData().put("model", "reviewer-agent-v1");
        return result;
    }
}
