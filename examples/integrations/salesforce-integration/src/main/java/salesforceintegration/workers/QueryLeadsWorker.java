package salesforceintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Queries leads from Salesforce.
 * Input: query
 * Output: leads, totalCount
 */
public class QueryLeadsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sfc_query_leads";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        System.out.println("  [query] Found 3 leads matching: " + query);

        java.util.List<java.util.Map<String, Object>> leads = java.util.List.of(
                java.util.Map.of("id", "lead-001", "name", "Acme Corp", "email", "info@acme.com", "industry", "Technology"),
                java.util.Map.of("id", "lead-002", "name", "Globex Inc", "email", "sales@globex.com", "industry", "Manufacturing"),
                java.util.Map.of("id", "lead-003", "name", "Initech LLC", "email", "contact@initech.com", "industry", "Finance"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("leads", leads);
        result.getOutputData().put("totalCount", leads.size());
        return result;
    }
}
