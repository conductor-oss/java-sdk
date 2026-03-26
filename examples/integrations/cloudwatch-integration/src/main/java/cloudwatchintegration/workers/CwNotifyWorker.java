package cloudwatchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends a CloudWatch alarm notification.
 * Input: alarmName, alarmState, email
 * Output: notified, sentAt
 */
public class CwNotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cw_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String alarmName = (String) task.getInputData().get("alarmName");
        String alarmState = (String) task.getInputData().get("alarmState");
        String email = (String) task.getInputData().get("email");
        System.out.println("  [notify] Alarm \"" + alarmName + "\" is " + alarmState + " -> notified " + email);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("sentAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
