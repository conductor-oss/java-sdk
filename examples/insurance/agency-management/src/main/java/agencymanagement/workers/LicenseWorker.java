package agencymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LicenseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agm_license";
    }

    @Override
    public TaskResult execute(Task task) {

        String agentId = (String) task.getInputData().get("agentId");
        System.out.printf("  [license] Agent %s licensed%n", agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("licenseNumber", "LIC-agency-management-CA");
        result.getOutputData().put("expiresAt", "2026-03-01");
        return result;
    }
}
