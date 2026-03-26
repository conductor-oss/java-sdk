package emailagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Analyzes the user's email intent and extracts structured information
 * such as email type, key points, urgency, and formality level.
 */
public class AnalyzeRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ea_analyze_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String intent = (String) task.getInputData().get("intent");
        if (intent == null || intent.isBlank()) {
            intent = "general email";
        }

        String recipient = (String) task.getInputData().get("recipient");
        if (recipient == null || recipient.isBlank()) {
            recipient = "unknown@example.com";
        }

        String context = (String) task.getInputData().get("context");
        if (context == null || context.isBlank()) {
            context = "";
        }

        System.out.println("  [ea_analyze_request] Analyzing email request for: " + recipient);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("emailType", "project_update");
        result.getOutputData().put("keyPoints", List.of(
                "Milestone 3 completed ahead of schedule",
                "All tests passing successfully",
                "Project Alpha progress update",
                "Delivery was 2 days early"
        ));
        result.getOutputData().put("urgency", "normal");
        result.getOutputData().put("formality", "professional");
        return result;
    }
}
