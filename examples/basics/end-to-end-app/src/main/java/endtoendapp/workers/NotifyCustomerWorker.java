package endtoendapp.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends a notification to the customer about their ticket status.
 */
public class NotifyCustomerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "notify_customer";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerEmail = (String) task.getInputData().get("customerEmail");
        String ticketId = (String) task.getInputData().get("ticketId");
        String priority = (String) task.getInputData().get("priority");
        String assignedTeam = (String) task.getInputData().get("assignedTeam");
        String estimatedResponse = (String) task.getInputData().get("estimatedResponse");

        System.out.println("  [notify_customer] Sending notification to " + customerEmail
                + " for ticket " + ticketId
                + " (Priority: " + priority + ", Team: " + assignedTeam
                + ", Response: " + estimatedResponse + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("channel", "email");
        return result;
    }
}
