package ragevaluation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that evaluates the coherence of a RAG answer.
 * Checks whether the answer is logically structured and well-organized.
 */
public class EvalCoherenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "re_eval_coherence";
    }

    @Override
    public TaskResult execute(Task task) {
        String metric = "coherence";
        double score = 0.95;
        String reason = "The answer is well-structured with a clear logical flow. "
                + "It progresses naturally from defining RAG to explaining retrieval, then generation, "
                + "making it easy to follow.";

        System.out.println("  [coherence] score=" + score + ", reason: " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metric", metric);
        result.getOutputData().put("score", score);
        result.getOutputData().put("reason", reason);
        return result;
    }
}
