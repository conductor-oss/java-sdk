package hubspotintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Enriches contact data.
 * Input: contactId, email, company
 * Output: industry, companySize, revenue, segment
 */
public class EnrichDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hs_enrich_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String contactId = (String) task.getInputData().get("contactId");
        System.out.println("  [enrich] Contact " + contactId + ": industry=SaaS, segment=mid-market");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("industry", "SaaS");
        result.getOutputData().put("companySize", "50-200");
        result.getOutputData().put("revenue", "$5M-$20M");
        result.getOutputData().put("segment", "mid-market");
        return result;
    }
}
