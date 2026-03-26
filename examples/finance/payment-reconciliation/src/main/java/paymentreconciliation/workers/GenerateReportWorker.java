package paymentreconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Generates a reconciliation report summarizing matched, resolved, and unresolved transactions.
 * Input: batchId, accountId, matchedCount, resolvedCount, unresolvedCount
 * Output: reportId, generatedAt, summary
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prc_generate_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String batchId = (String) task.getInputData().get("batchId");
        if (batchId == null) batchId = "UNKNOWN";
        String accountId = (String) task.getInputData().get("accountId");

        Object matchedCount = task.getInputData().get("matchedCount");
        Object resolvedCount = task.getInputData().get("resolvedCount");
        Object unresolvedCount = task.getInputData().get("unresolvedCount");

        System.out.println("  [report] Generating reconciliation report for batch " + batchId
                + " | account=" + accountId + " matched=" + matchedCount
                + " resolved=" + resolvedCount + " unresolved=" + unresolvedCount);

        String reportId = "REC-RPT-" + batchId + "-" + System.currentTimeMillis();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", reportId);
        result.getOutputData().put("generatedAt", Instant.now().toString());
        result.getOutputData().put("summary", "Reconciliation complete: "
                + matchedCount + " matched, " + resolvedCount + " resolved, "
                + unresolvedCount + " pending review");
        return result;
    }
}
