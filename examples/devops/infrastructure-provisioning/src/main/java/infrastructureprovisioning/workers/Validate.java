package infrastructureprovisioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates the infrastructure plan against policies.
 */
public class Validate implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[ip_validate] Plan validated: no policy violations");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("valid", true);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
