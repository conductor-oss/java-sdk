package configmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ValidateConfigWorker implements Worker {
    @Override public String getTaskDefName() { return "cf_validate_config"; }

    @Override public TaskResult execute(Task task) {
        String schema = (String) task.getInputData().getOrDefault("schema", "unknown");
        System.out.println("  [validate] Validating config against schema " + schema + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("errors", List.of());
        result.getOutputData().put("warnings", List.of("logLevel 'info' may be verbose for production"));
        return result;
    }
}
