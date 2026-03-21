package containerorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Configures auto-scaling for a deployment.
 * Input: serviceName, deploymentId, minReplicas, maxReplicas
 * Output: configured, policy
 */
public class CtrScaleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctr_scale";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown";
        Object minR = task.getInputData().get("minReplicas");
        Object maxR = task.getInputData().get("maxReplicas");

        System.out.println("  [scale] Auto-scaling " + serviceName + ": " + minR + "-" + maxR + " replicas");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("configured", true);
        result.getOutputData().put("policy", "cpu-target-70");
        return result;
    }
}
