package selfrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Refines the query when quality gate fails.
 * Returns {refinedQuery}.
 */
public class RefineRetryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_refine_retry";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [retry] Quality too low - would re-retrieve with refined query");

        String question = (String) task.getInputData().get("question");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("refinedQuery", question + " (refined)");
        return result;
    }
}
