package interservicecommunication.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotificationServiceWorker implements Worker {
    @Override public String getTaskDefName() { return "isc_notification_service"; }
    @Override public TaskResult execute(Task task) {
        String trackingId = (String) task.getInputData().getOrDefault("trackingId", "unknown");
        System.out.println("  [notify] Sent tracking " + trackingId + " to customer");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("sent", true);
        r.getOutputData().put("channel", "email");
        return r;
    }
}
