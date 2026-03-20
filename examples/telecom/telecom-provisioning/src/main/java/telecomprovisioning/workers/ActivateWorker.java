package telecomprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ActivateWorker implements Worker {
    @Override public String getTaskDefName() { return "tpv_activate"; }

    @Override
    public TaskResult execute(Task task) {
        String configId = (String) task.getInputData().get("configId");
        System.out.printf("  [activate] Service activated — config %s%n", configId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("serviceId", "SVC-telecom-provisioning-001");
        result.getOutputData().put("activatedAt", "2024-03-10T10:00:00Z");
        return result;
    }
}
