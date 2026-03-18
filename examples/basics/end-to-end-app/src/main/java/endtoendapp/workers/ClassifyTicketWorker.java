package endtoendapp.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Classifies a support ticket's priority based on keyword matching
 * in the subject and description fields.
 */
public class ClassifyTicketWorker implements Worker {

    private static final Map<String, List<String>> PRIORITY_KEYWORDS = Map.of(
            "CRITICAL", List.of("down", "outage", "crash", "production", "emergency", "security breach"),
            "HIGH", List.of("urgent", "broken", "cannot", "blocker", "data loss"),
            "MEDIUM", List.of("error", "bug", "issue", "not working", "slow"),
            "LOW", List.of("question", "feature request", "how to", "suggestion", "minor")
    );

    private static final List<String> PRIORITY_ORDER = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW");

    @Override
    public String getTaskDefName() {
        return "classify_ticket";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String subject = (String) task.getInputData().get("subject");
        String description = (String) task.getInputData().get("description");

        String text = ((subject != null ? subject : "") + " " + (description != null ? description : "")).toLowerCase();

        String priority = "LOW";
        List<String> keywordsMatched = new ArrayList<>();

        for (String level : PRIORITY_ORDER) {
            List<String> keywords = PRIORITY_KEYWORDS.get(level);
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    if (keywordsMatched.isEmpty()) {
                        priority = level;
                    }
                    keywordsMatched.add(keyword);
                }
            }
            if (!keywordsMatched.isEmpty()) {
                break;
            }
        }

        System.out.println("  [classify_ticket] Ticket " + ticketId + " classified as " + priority);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priority", priority);
        result.getOutputData().put("keywordsMatched", keywordsMatched);
        return result;
    }
}
