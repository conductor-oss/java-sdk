package appointmentscheduling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends appointment confirmation to the patient.
 * Input: patientId, appointmentId, slot
 * Output: confirmed, channel
 */
public class ConfirmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "apt_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        String appointmentId = (String) task.getInputData().get("appointmentId");
        if (appointmentId == null) appointmentId = "UNKNOWN";

        System.out.println("  [confirm] Confirmation sent for appointment " + appointmentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmed", true);
        result.getOutputData().put("channel", "email+sms");
        return result;
    }
}
