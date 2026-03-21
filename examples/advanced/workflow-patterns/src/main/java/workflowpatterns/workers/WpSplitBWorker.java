package workflowpatterns.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Split branch B: parallel fork processing.
 * Input: branch, chainOutput
 * Output: result, value
 */
public class WpSplitBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wp_split_b";
    }

    @Override
    public TaskResult execute(Task task) {
        String chainOutput = (String) task.getInputData().get("chainOutput");
        if (chainOutput == null) {
            chainOutput = "unknown";
        }

        System.out.println("  [split-B] Branch B processing (from " + chainOutput + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "branch_b_done");
        result.getOutputData().put("value", 58);
        return result;
    }
}
