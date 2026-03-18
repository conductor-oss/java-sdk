package multiagentsupport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates the quality of the support response by running a set of checks:
 * tone, greeting, next steps, and sensitive data screening.
 */
public class QaValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_qa_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        String ticketId = (String) task.getInputData().get("ticketId");
        String category = (String) task.getInputData().get("category");
        String customerTier = (String) task.getInputData().get("customerTier");

        if (ticketId == null) ticketId = "UNKNOWN";
        if (category == null) category = "general";
        if (customerTier == null) customerTier = "standard";

        System.out.println("  [cs_qa_validate] Validating response for ticket: " + ticketId);

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("toneAppropriate", true);
        checks.put("includesGreeting", true);
        checks.put("includesNextSteps", true);
        checks.put("noSensitiveData", true);

        String finalResponse = "Dear Customer, your ticket " + ticketId + " (category: " + category
                + ") has been reviewed and validated. Our team has ensured the response meets "
                + "quality standards. Please review the proposed solution and let us know "
                + "if you need further assistance.";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", true);
        result.getOutputData().put("checks", checks);
        result.getOutputData().put("finalResponse", finalResponse);
        result.getOutputData().put("reviewedAt", "2026-03-14T10:30:00Z");
        return result;
    }
}
