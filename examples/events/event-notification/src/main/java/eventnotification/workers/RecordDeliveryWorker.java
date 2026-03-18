package eventnotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Records the delivery status across all notification channels.
 * Input: emailStatus, smsStatus, pushStatus
 * Output: overallStatus ("all_delivered" if all "sent", otherwise "partial_delivery")
 */
public class RecordDeliveryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_record_delivery";
    }

    @Override
    public TaskResult execute(Task task) {
        String emailStatus = (String) task.getInputData().get("emailStatus");
        String smsStatus = (String) task.getInputData().get("smsStatus");
        String pushStatus = (String) task.getInputData().get("pushStatus");

        boolean allSent = "sent".equals(emailStatus)
                && "sent".equals(smsStatus)
                && "sent".equals(pushStatus);

        String overallStatus = allSent ? "all_delivered" : "partial_delivery";

        System.out.println("  [en_record_delivery] email=" + emailStatus
                + ", sms=" + smsStatus + ", push=" + pushStatus
                + " -> " + overallStatus);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallStatus", overallStatus);
        return result;
    }
}
