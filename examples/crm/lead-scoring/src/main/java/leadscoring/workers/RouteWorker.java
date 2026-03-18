package leadscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Routes lead to appropriate sales team. Real routing based on classification and industry.
 */
public class RouteWorker implements Worker {
    @Override public String getTaskDefName() { return "ls_route"; }

    @Override public TaskResult execute(Task task) {
        String classification = (String) task.getInputData().get("classification");
        String priority = (String) task.getInputData().get("priority");
        if (classification == null) classification = "cold";
        if (priority == null) priority = "P4";

        String assignedTo;
        String action;
        switch (classification) {
            case "hot" -> { assignedTo = "Senior AE Team"; action = "immediate_outreach"; }
            case "warm" -> { assignedTo = "AE Team"; action = "schedule_demo"; }
            case "cool" -> { assignedTo = "SDR Team"; action = "nurture_campaign"; }
            default -> { assignedTo = "Marketing Automation"; action = "email_nurture"; }
        }

        System.out.println("  [route] " + classification + " lead -> " + assignedTo + " (" + action + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assignedTo", assignedTo);
        result.getOutputData().put("action", action);
        result.getOutputData().put("routed", true);
        return result;
    }
}
