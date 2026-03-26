package infrastructureprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provisions the infrastructure resource.
 */
public class Provision implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_provision";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[ip_provision] Resource created");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("resourceId", "RES-500001");
        output.put("status", "running");
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
