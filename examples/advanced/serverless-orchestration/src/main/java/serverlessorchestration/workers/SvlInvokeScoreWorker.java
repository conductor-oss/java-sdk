package serverlessorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Invokes the score serverless function to compute engagement score.
 * Input: functionArn, enrichedData
 * Output: score, confidence, billedMs
 */
public class SvlInvokeScoreWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "svl_invoke_score";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [lambda:score] Computing engagement score");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", 87.5);
        result.getOutputData().put("confidence", 0.92);
        result.getOutputData().put("billedMs", 80);
        return result;
    }
}
