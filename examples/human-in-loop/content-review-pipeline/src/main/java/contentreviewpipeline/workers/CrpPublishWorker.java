package contentreviewpipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for crp_publish task -- publishes content if approved.
 *
 * Inputs:
 *   - approved: boolean indicating whether the content was approved
 *   - content:  the content to publish
 *
 * Outputs:
 *   - published: true if approved and published, false otherwise
 *   - url:       the published URL (only present when published=true)
 */
public class CrpPublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crp_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        Object approvedObj = task.getInputData().get("approved");
        boolean approved = Boolean.TRUE.equals(approvedObj);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (approved) {
            result.getOutputData().put("published", true);
            result.getOutputData().put("url", "https://example.com/articles/published");
            System.out.println("  [crp_publish] Content published at https://example.com/articles/published");
        } else {
            result.getOutputData().put("published", false);
            System.out.println("  [crp_publish] Content not approved, skipping publish.");
        }

        return result;
    }
}
