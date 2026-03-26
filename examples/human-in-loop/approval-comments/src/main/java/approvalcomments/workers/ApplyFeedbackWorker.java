package approvalcomments.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Worker for ac_apply_feedback -- applies the reviewer's feedback.
 *
 * Reads the decision, comments, attachments, rating, and tags from the
 * input (populated from the WAIT task output) and logs them.
 * Returns { applied: true }.
 */
public class ApplyFeedbackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_apply_feedback";
    }

    @Override
    public TaskResult execute(Task task) {
        String decision = task.getInputData().get("decision") != null
                ? task.getInputData().get("decision").toString() : "none";
        String comments = task.getInputData().get("comments") != null
                ? task.getInputData().get("comments").toString() : "";

        Object attachmentsObj = task.getInputData().get("attachments");
        @SuppressWarnings("unchecked")
        List<String> attachments = attachmentsObj instanceof List ? (List<String>) attachmentsObj : List.of();

        Object ratingObj = task.getInputData().get("rating");
        int rating = ratingObj instanceof Number ? ((Number) ratingObj).intValue() : 0;

        Object tagsObj = task.getInputData().get("tags");
        @SuppressWarnings("unchecked")
        List<String> tags = tagsObj instanceof List ? (List<String>) tagsObj : List.of();

        System.out.println("  [ac_apply_feedback] Decision: " + decision);
        System.out.println("  [ac_apply_feedback] Comments: " + comments);
        System.out.println("  [ac_apply_feedback] Attachments: " + attachments);
        System.out.println("  [ac_apply_feedback] Rating: " + rating);
        System.out.println("  [ac_apply_feedback] Tags: " + tags);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applied", true);
        return result;
    }
}
