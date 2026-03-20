package eventmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FollowupWorker implements Worker {
    @Override public String getTaskDefName() { return "evt_followup"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [followup] Post-event surveys sent to " + task.getInputData().get("attendees") + " attendees");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("surveySent", true);
        result.getOutputData().put("satisfaction", 4.6);
        result.getOutputData().put("nps", 72);
        result.getOutputData().put("followUpEmails", task.getInputData().get("attendees"));
        return result;
    }
}
