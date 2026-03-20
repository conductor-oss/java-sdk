package servicemeshorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deploys a sidecar proxy for a service in a given namespace.
 * Input: serviceName, namespace
 * Output: sidecarId, version
 */
public class DeploySidecarWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mesh_deploy_sidecar";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        String namespace = (String) task.getInputData().get("namespace");
        if (serviceName == null) serviceName = "unknown";
        if (namespace == null) namespace = "default";

        System.out.println("  [sidecar] Deploying proxy for " + serviceName + " in " + namespace);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sidecarId", "envoy-" + serviceName.hashCode());
        result.getOutputData().put("version", "1.28");
        return result;
    }
}
