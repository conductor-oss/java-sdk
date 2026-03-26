package finetuneddeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends notifications about pipeline status (completion or failure).
 */
public class NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ftd_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String reason = (String) task.getInputData().get("reason");
        String modelId = (String) task.getInputData().get("modelId");

        System.out.println("  [notify] " + reason + " — model: " + modelId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        return result;
    }
}
