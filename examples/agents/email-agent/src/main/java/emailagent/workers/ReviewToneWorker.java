package emailagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Reviews the drafted email's tone against the desired tone. Returns
 * a tone score, approval status, analysis breakdown, and suggestions.
 */
public class ReviewToneWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ea_review_tone";
    }

    @Override
    public TaskResult execute(Task task) {
        String subject = (String) task.getInputData().get("subject");
        if (subject == null || subject.isBlank()) {
            subject = "(no subject)";
        }

        String body = (String) task.getInputData().get("body");
        if (body == null || body.isBlank()) {
            body = "";
        }

        String desiredTone = (String) task.getInputData().get("desiredTone");
        if (desiredTone == null || desiredTone.isBlank()) {
            desiredTone = "professional";
        }

        System.out.println("  [ea_review_tone] Reviewing tone for: " + subject);

        Map<String, Object> analysis = Map.of(
                "professionalism", 0.94,
                "clarity", 0.89,
                "warmth", 0.78,
                "conciseness", 0.85
        );

        List<String> suggestions = List.of(
                "Consider adding a specific date for the Milestone 4 kickoff",
                "The closing could include a call-to-action for next steps"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalSubject", subject);
        result.getOutputData().put("finalBody", body);
        result.getOutputData().put("toneScore", 0.91);
        result.getOutputData().put("toneApproved", "true");
        result.getOutputData().put("analysis", analysis);
        result.getOutputData().put("suggestions", suggestions);
        return result;
    }
}
