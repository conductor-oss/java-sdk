package environmentmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configures the environment.
 */
public class ConfigureEnv implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_configure";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[em_configure] Applied 23 settings");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("configured", true);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
