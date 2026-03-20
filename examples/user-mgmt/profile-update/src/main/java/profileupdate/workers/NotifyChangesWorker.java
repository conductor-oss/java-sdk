package profileupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Notifies the user of profile changes.
 * Input: userId, changes
 * Output: notified
 */
public class NotifyChangesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfu_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [notify] User notified of profile changes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        return result;
    }
}
