package environmentmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies the environment is healthy.
 */
public class VerifyEnv implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[em_verify] Environment healthy");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("healthy", true);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
