package vendoronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Evaluates the vendor and assigns a score.
 * Input: vendorId, verificationResult
 * Output: score, tier
 */
public class EvaluateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "von_evaluate";
    }

    @Override
    public TaskResult execute(Task task) {
        int score = 88;

        System.out.println("  [evaluate] Vendor score: " + score + "/100");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("tier", "gold");
        return result;
    }
}
