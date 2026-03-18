package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Confirms GDPR request completion and generates compliance report.
 */
public class ConfirmCompletionWorker implements Worker {
    @Override public String getTaskDefName() { return "gdpr_confirm_completion"; }

    @Override public TaskResult execute(Task task) {
        String requestType = (String) task.getInputData().get("requestType");
        Object piiCountObj = task.getInputData().get("piiCount");
        if (requestType == null) requestType = "access";
        int piiCount = 0;
        if (piiCountObj instanceof Number) piiCount = ((Number) piiCountObj).intValue();

        String reportId = "GDPR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [gdpr_confirm] " + requestType + " request completed. Report: " + reportId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("completed", true);
        result.getOutputData().put("reportId", reportId);
        result.getOutputData().put("piiItemsProcessed", piiCount);
        result.getOutputData().put("completedAt", Instant.now().toString());
        return result;
    }
}
