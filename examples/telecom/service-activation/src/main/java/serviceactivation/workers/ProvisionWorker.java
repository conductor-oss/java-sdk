package serviceactivation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ProvisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sac_provision";
    }

    @Override
    public TaskResult execute(Task task) {

        String serviceType = (String) task.getInputData().get("serviceType");
        System.out.printf("  [provision] %s service provisioned%n", serviceType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("serviceId", "SVC-service-activation-001");
        result.getOutputData().put("endpoint", "10.0.77.1");
        return result;
    }
}
