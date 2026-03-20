package zendeskintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Classifies ticket priority.
 * Input: ticketId, subject, description
 * Output: priority, sentiment, language
 */
public class ClassifyTicketWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "zd_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String subject = (String) task.getInputData().get("subject");
        String priority = subject != null && subject.toLowerCase().contains("urgent") ? "high" : "normal";
        System.out.println("  [classify] Ticket " + ticketId + " -> priority: " + priority);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priority", "" + priority);
        result.getOutputData().put("sentiment", "neutral");
        result.getOutputData().put("language", "en");
        return result;
    }
}
