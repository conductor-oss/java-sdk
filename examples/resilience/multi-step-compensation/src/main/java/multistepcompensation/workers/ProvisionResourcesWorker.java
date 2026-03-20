package multistepcompensation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for msc_provision_resources — provisions resources.
 *
 * If failAt input equals "provision", the task returns FAILED status.
 * Otherwise, it succeeds and returns resourceId="RES-001".
 */
public class ProvisionResourcesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msc_provision_resources";
    }

    @Override
    public TaskResult execute(Task task) {
        String failAt = (String) task.getInputData().get("failAt");

        System.out.println("  [msc_provision_resources] Provisioning resources (failAt=" + failAt + ")...");

        TaskResult result = new TaskResult(task);

        if ("provision".equals(failAt)) {
            System.out.println("  [msc_provision_resources] FAILED — deterministic.failure at provision step");
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Provisioning failed");
        } else {
            System.out.println("  [msc_provision_resources] Resources provisioned: RES-001");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("resourceId", "RES-001");
        }

        return result;
    }
}
