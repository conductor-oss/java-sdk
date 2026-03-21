package automatedtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sets up the test environment.
 */
public class SetupEnv implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_setup_env";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[at_setup_env] Test environment ready");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("buildId", "BLD-200001");
        output.put("ready", true);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
