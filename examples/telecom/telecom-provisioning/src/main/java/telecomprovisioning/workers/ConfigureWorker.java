package telecomprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfigureWorker implements Worker {
    @Override public String getTaskDefName() { return "tpv_configure"; }

    @Override
    public TaskResult execute(Task task) {
        String serviceType = (String) task.getInputData().get("serviceType");
        System.out.printf("  [configure] Network configured for %s%n", serviceType);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("configId", "CFG-telecom-provisioning-A");
        result.getOutputData().put("bandwidth", "100Mbps");
        return result;
    }
}
