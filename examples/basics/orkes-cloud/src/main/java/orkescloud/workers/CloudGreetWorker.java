package orkescloud.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that greets a user with a cloud/local indicator.
 *
 * In cloud mode, the task name is "cloud_greet" and the greeting
 * includes "[Cloud]". In local mode, the task name is "local_greet"
 * and the greeting includes "[Local]".
 */
public class CloudGreetWorker implements Worker {

    private final String taskDefName;
    private final boolean cloudMode;

    public CloudGreetWorker(boolean cloudMode) {
        this.cloudMode = cloudMode;
        this.taskDefName = cloudMode ? "cloud_greet" : "local_greet";
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("name");
        if (name == null || name.isBlank()) {
            name = "World";
        }

        String modeLabel = cloudMode ? "Cloud" : "Local";
        String greeting = "Hello, " + name + "! Connected via Orkes " + modeLabel + " mode.";

        System.out.println("  [" + taskDefName + " worker] " + greeting);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("greeting", greeting);
        result.getOutputData().put("mode", modeLabel);
        return result;
    }
}
