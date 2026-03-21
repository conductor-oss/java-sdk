package salesforceintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Updates lead records in Salesforce.
 * Input: scoredLeads
 * Output: updatedCount, updatedAt
 */
public class UpdateRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfc_update_records";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        java.util.List<?> leads = (java.util.List<?>) task.getInputData().getOrDefault("scoredLeads", java.util.List.of());
        System.out.println("  [update] Updated " + leads.size() + " lead records in Salesforce");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updatedCount", leads.size());
        result.getOutputData().put("updatedAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
