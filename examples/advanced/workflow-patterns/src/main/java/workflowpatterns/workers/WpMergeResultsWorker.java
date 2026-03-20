package workflowpatterns.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Merge results from fork branches A and B.
 * Input: resultA, resultB
 * Output: combined, total
 */
public class WpMergeResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wp_merge_results";
    }

    @Override
    public TaskResult execute(Task task) {
        String resultA = (String) task.getInputData().get("resultA");
        String resultB = (String) task.getInputData().get("resultB");
        if (resultA == null) resultA = "unknown_a";
        if (resultB == null) resultB = "unknown_b";

        System.out.println("  [merge] Combining: " + resultA + " + " + resultB);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("combined", "merged_a_b");
        result.getOutputData().put("total", 100);
        return result;
    }
}
