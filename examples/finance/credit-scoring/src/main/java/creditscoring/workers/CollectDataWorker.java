package creditscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Collects credit history data for an applicant.
 * Input: applicantId, ssn
 * Output: creditHistory
 */
public class CollectDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "csc_collect_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        if (applicantId == null) applicantId = "unknown";

        System.out.println("  [collect] Pulling credit data for applicant " + applicantId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("creditHistory", Map.of(
                "accountAge", 12,
                "totalAccounts", 8,
                "openAccounts", 5,
                "latePay30", 1,
                "latePay60", 0,
                "collections", 0,
                "utilization", 28,
                "inquiries", 2,
                "publicRecords", 0));
        return result;
    }
}
