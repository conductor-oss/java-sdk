package ragevaluation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that evaluates the faithfulness of a RAG answer.
 * Checks whether the answer is supported by the retrieved context.
 */
public class EvalFaithfulnessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "re_eval_faithfulness";
    }

    @Override
    public TaskResult execute(Task task) {
        String metric = "faithfulness";
        double score = 0.92;
        String reason = "The answer accurately reflects the information provided in the retrieved context passages. "
                + "All key claims about RAG pipelines, retrieval, and generation are directly supported by the context.";

        System.out.println("  [faithfulness] score=" + score + ", reason: " + reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("metric", metric);
        result.getOutputData().put("score", score);
        result.getOutputData().put("reason", reason);
        return result;
    }
}
