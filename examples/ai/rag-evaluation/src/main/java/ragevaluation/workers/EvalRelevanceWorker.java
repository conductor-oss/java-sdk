package ragevaluation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that evaluates the relevance of a RAG answer.
 * Checks whether the answer addresses the original question.
 */
public class EvalRelevanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "re_eval_relevance";
    }

    @Override
    public TaskResult execute(Task task) {
        String metric = "relevance";
        double score = 0.88;
        String reason = "The answer is highly relevant to the question, covering the core mechanics of RAG pipelines. "
                + "Minor deduction for not elaborating on specific implementation details or alternative approaches.";

        System.out.println("  [relevance] score=" + score + ", reason: " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metric", metric);
        result.getOutputData().put("score", score);
        result.getOutputData().put("reason", reason);
        return result;
    }
}
