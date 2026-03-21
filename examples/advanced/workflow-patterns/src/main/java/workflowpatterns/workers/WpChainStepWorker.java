package workflowpatterns.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Chain step: sequential processing in a chain pattern.
 * Input: step, data
 * Output: result
 */
public class WpChainStepWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wp_chain_step";
    }

    @Override
    public TaskResult execute(Task task) {
        String data = (String) task.getInputData().get("data");
        if (data == null) {
            data = "unknown";
        }

        System.out.println("  [chain] Processing: " + data);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "chain_processed");
        return result;
    }
}
