package vendoronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Activates the vendor in the system.
 * Input: vendorId, approved
 * Output: active, activatedAt
 */
public class ActivateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "von_activate";
    }

    @Override
    public TaskResult execute(Task task) {
        String vendorId = (String) task.getInputData().get("vendorId");

        System.out.println("  [activate] Vendor " + vendorId + " activated in system");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("active", true);
        result.getOutputData().put("activatedAt", Instant.now().toString());
        return result;
    }
}
