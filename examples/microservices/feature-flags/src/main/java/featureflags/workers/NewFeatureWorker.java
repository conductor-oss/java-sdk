package featureflags.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NewFeatureWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_new_feature"; }

    @Override public TaskResult execute(Task task) {
        String feature = (String) task.getInputData().getOrDefault("feature", "unknown");
        System.out.println("  [new] Executing new feature path for \"" + feature + "\"...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("path", "new");
        result.getOutputData().put("uiVersion", "v2");
        result.getOutputData().put("rendered", true);
        return result;
    }
}
