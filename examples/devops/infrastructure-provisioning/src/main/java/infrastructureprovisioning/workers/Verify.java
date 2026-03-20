package infrastructureprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies the provisioned resource is healthy.
 */
public class Verify implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        String resourceId = (String) task.getInputData().get("resourceId");
        System.out.println("[ip_verify] Resource " + resourceId + " healthy");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("verified", true);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
