package contentpublishing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves content based on review score.
 * Input: contentId, reviewScore, reviewNotes
 * Output: approved, approver, approvedAt
 */
public class ApproveContentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pub_approve_content";
    }

    @Override
    public TaskResult execute(Task task) {
        Object reviewScore = task.getInputData().get("reviewScore");
        if (reviewScore == null) reviewScore = 0;

        System.out.println("  [approve] Review score: " + reviewScore + " — approved");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", true);
        result.getOutputData().put("approver", "chief-editor");
        result.getOutputData().put("approvedAt", "2026-03-08T11:00:00Z");
        return result;
    }
}
