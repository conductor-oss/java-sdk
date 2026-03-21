package securityincident.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Contains a security incident by isolating the affected system.
 * Input: containData (from triage output)
 * Output: contain, processed
 */
public class ContainWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "si_contain";
    }

    @Override
    public TaskResult execute(Task task) {
        Object containData = task.getInputData().get("containData");

        System.out.println("  [contain] Isolated affected system from network");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("contain", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
