package containerorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deploys a container image with the specified replica count.
 * Input: serviceName, imageUri, replicas
 * Output: deploymentId, replicas, status
 */
public class CtrDeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctr_deploy";
    }

    @Override
    public TaskResult execute(Task task) {
        String imageUri = (String) task.getInputData().get("imageUri");
        if (imageUri == null) imageUri = "unknown";

        int replicas = 3;
        Object replicasObj = task.getInputData().get("replicas");
        if (replicasObj instanceof Number) {
            replicas = ((Number) replicasObj).intValue();
        } else if (replicasObj instanceof String) {
            try { replicas = Integer.parseInt((String) replicasObj); } catch (NumberFormatException ignored) {}
        }

        System.out.println("  [deploy] Deploying " + imageUri + " with " + replicas + " replicas");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deploymentId", "deploy-20260314");
        result.getOutputData().put("replicas", replicas);
        result.getOutputData().put("status", "running");
        return result;
    }
}
