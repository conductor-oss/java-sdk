package vendoronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Submits a vendor application.
 * Input: vendorName, category, country
 * Output: vendorId
 */
public class ApplyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "von_apply";
    }

    @Override
    public TaskResult execute(Task task) {
        String vendorName = (String) task.getInputData().get("vendorName");
        String category = (String) task.getInputData().get("category");

        System.out.println("  [apply] Vendor application: " + vendorName + " (" + category + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("vendorId", "VND-653-001");
        return result;
    }
}
