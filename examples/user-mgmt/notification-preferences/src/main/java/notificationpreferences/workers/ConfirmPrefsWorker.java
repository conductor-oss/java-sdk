package notificationpreferences.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends confirmation for updated notification preferences.
 * Input: userId, channels
 * Output: confirmed
 */
public class ConfirmPrefsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "np_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [confirm] Confirmation sent for updated notification preferences");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmed", true);
        return result;
    }
}
