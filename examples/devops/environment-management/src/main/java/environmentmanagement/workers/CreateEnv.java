package environmentmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a new environment.
 */
public class CreateEnv implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_create_env";
    }

    @Override
    public TaskResult execute(Task task) {
        String envName = (String) task.getInputData().get("envName");
        System.out.println("[em_create_env] Environment " + envName + " created");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("envId", "ENV-300001");
        output.put("created", true);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
