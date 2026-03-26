package vendoronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Approves or rejects a vendor based on score.
 * Input: vendorId, score
 * Output: approved
 */
public class ApproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "von_approve";
    }

    @Override
    public TaskResult execute(Task task) {
        Object scoreObj = task.getInputData().get("score");
        int score = scoreObj instanceof Number ? ((Number) scoreObj).intValue() : 0;
        boolean approved = score >= 70;

        System.out.println("  [approve] Vendor " + (approved ? "approved" : "rejected") + " (score: " + score + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", approved);
        return result;
    }
}
