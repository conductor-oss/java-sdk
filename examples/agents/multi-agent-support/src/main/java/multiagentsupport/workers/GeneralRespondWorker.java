package multiagentsupport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Handles general inquiries by providing a helpful response and suggesting documentation.
 */
public class GeneralRespondWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_general_respond";
    }

    @Override
    public TaskResult execute(Task task) {
        String subject = (String) task.getInputData().get("subject");
        String description = (String) task.getInputData().get("description");

        if (subject == null) subject = "";
        if (description == null) description = "";

        System.out.println("  [cs_general_respond] Handling general inquiry: " + subject);

        String response = "Thank you for reaching out to our support team. "
                + "Based on your inquiry, we recommend reviewing the following resources. "
                + "If you need further assistance, please don't hesitate to reply to this ticket "
                + "or contact us via live chat during business hours.";

        List<String> suggestedDocs = List.of(
                "Getting Started Guide",
                "FAQ - Frequently Asked Questions",
                "API Documentation"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("response", response);
        result.getOutputData().put("suggestedDocs", suggestedDocs);
        return result;
    }
}
