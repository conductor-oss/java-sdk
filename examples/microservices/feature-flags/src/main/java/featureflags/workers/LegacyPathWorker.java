package featureflags.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LegacyPathWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_legacy_path"; }

    @Override public TaskResult execute(Task task) {
        String feature = (String) task.getInputData().getOrDefault("feature", "unknown");
        System.out.println("  [legacy] Executing legacy path for \"" + feature + "\"...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("path", "legacy");
        result.getOutputData().put("uiVersion", "v1");
        result.getOutputData().put("rendered", true);
        return result;
    }
}
