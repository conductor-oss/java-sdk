package datasampling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Flags a dataset for review when quality checks fail.
 * Input: totalRecords (int), qualityScore (double), issues (list of strings)
 * Output: approved (false), status ("FLAGGED_FOR_REVIEW")
 */
public class FlagForReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sm_flag_for_review";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object totalRecords = task.getInputData().get("totalRecords");
        Object qualityScore = task.getInputData().get("qualityScore");
        List<String> issues = (List<String>) task.getInputData().get("issues");
        if (issues == null) {
            issues = List.of();
        }

        System.out.println("  [sm_flag_for_review] Flagging dataset: " + totalRecords
                + " records, quality score: " + qualityScore + ", issues: " + issues.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", false);
        result.getOutputData().put("status", "FLAGGED_FOR_REVIEW");
        return result;
    }
}
