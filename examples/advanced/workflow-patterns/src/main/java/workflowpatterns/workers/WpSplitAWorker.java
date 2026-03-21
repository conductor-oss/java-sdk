package workflowpatterns.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Split branch A: parallel fork processing.
 * Input: branch, chainOutput
 * Output: result, value
 */
public class WpSplitAWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wp_split_a";
    }

    @Override
    public TaskResult execute(Task task) {
        String chainOutput = (String) task.getInputData().get("chainOutput");
        if (chainOutput == null) {
            chainOutput = "unknown";
        }

        System.out.println("  [split-A] Branch A processing (from " + chainOutput + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "branch_a_done");
        result.getOutputData().put("value", 42);
        return result;
    }
}
