package infrastructureprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates an infrastructure provisioning plan.
 */
public class Plan implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_plan";
    }

    @Override
    public TaskResult execute(Task task) {
        String resourceType = (String) task.getInputData().get("resourceType");
        String region = (String) task.getInputData().get("region");

        System.out.println("[ip_plan] Planning " + resourceType + " in " + region);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("type", resourceType);
        plan.put("region", region);
        plan.put("size", "medium");
        output.put("plan", plan);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
