package penetrationtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Performs reconnaissance on the target system.
 * Input: target, scope
 * Output: reconnaissanceId, success
 */
public class ReconnaissanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pen_reconnaissance";
    }

    @Override
    public TaskResult execute(Task task) {
        String target = (String) task.getInputData().get("target");
        if (target == null) {
            target = "unknown";
        }

        System.out.println("  [recon] " + target + ": 12 endpoints, 4 open ports discovered");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reconnaissanceId", "RECONNAISSANCE-1382");
        result.getOutputData().put("success", true);
        return result;
    }
}
