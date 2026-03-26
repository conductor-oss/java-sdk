package securityincident.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Investigates a security incident to determine root cause.
 * Input: investigateData (from contain output)
 * Output: investigate, processed
 */
public class InvestigateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "si_investigate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [investigate] Root cause: compromised API key used from unauthorized IP");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("investigate", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
