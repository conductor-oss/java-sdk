package ragqualitygates.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that handles rejection when a quality gate fails.
 * Takes a reason and score, and returns rejected=true along
 * with the reason and score.
 */
public class RejectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qg_reject";
    }

    @Override
    public TaskResult execute(Task task) {
        String reason = (String) task.getInputData().get("reason");
        if (reason == null) {
            reason = "unknown";
        }

        Object scoreObj = task.getInputData().get("score");
        double score = 0.0;
        if (scoreObj instanceof Number) {
            score = ((Number) scoreObj).doubleValue();
        }

        System.out.println("  [reject] Rejecting: reason=" + reason + ", score=" + score);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rejected", true);
        result.getOutputData().put("reason", reason);
        result.getOutputData().put("score", score);
        return result;
    }
}
