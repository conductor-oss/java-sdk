package servicemeshorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sets traffic policy for a service mesh.
 * Input: serviceName, meshType
 * Output: applied, retries, timeout
 */
public class SetTrafficPolicyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mesh_set_traffic_policy";
    }

    @Override
    public TaskResult execute(Task task) {
        String meshType = (String) task.getInputData().get("meshType");
        if (meshType == null) meshType = "unknown";

        System.out.println("  [policy] Setting traffic policy: " + meshType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applied", true);
        result.getOutputData().put("retries", 3);
        result.getOutputData().put("timeout", "30s");
        return result;
    }
}
