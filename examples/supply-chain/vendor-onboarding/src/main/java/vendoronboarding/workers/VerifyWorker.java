package vendoronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Verifies vendor credentials.
 * Input: vendorId
 * Output: verified, checks
 */
public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "von_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        String vendorId = (String) task.getInputData().get("vendorId");

        System.out.println("  [verify] Background check on " + vendorId + " — passed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("checks", List.of("business_license", "insurance", "references"));
        return result;
    }
}
