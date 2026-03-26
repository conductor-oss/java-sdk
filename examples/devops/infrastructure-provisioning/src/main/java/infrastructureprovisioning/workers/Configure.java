package infrastructureprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies configuration to the provisioned resource.
 */
public class Configure implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_configure";
    }

    @Override
    public TaskResult execute(Task task) {
        String resourceId = (String) task.getInputData().get("resourceId");
        System.out.println("[ip_configure] Applied configuration to " + resourceId);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("configured", true);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
