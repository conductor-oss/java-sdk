package featureflags.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DefaultPathWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_default_path"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [default] Unknown flag status, using default path...");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("path", "default");
        result.getOutputData().put("uiVersion", "v1");
        result.getOutputData().put("rendered", true);
        return result;
    }
}
