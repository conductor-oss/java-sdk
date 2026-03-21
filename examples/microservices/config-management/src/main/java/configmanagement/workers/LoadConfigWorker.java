package configmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class LoadConfigWorker implements Worker {
    @Override public String getTaskDefName() { return "cf_load_config"; }

    @Override public TaskResult execute(Task task) {
        String src = (String) task.getInputData().getOrDefault("configSource", "unknown");
        String env = (String) task.getInputData().getOrDefault("environment", "unknown");
        System.out.println("  [load] Loading config from " + src + " for " + env + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("config", Map.of("dbPoolSize", 20, "cacheEnabled", true, "logLevel", "info", "maxRetries", 3, "timeoutMs", 5000));
        result.getOutputData().put("schema", "app-config-v2");
        result.getOutputData().put("version", "1.4.0");
        return result;
    }
}
