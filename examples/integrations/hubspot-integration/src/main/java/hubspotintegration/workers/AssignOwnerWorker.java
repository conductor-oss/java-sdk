package hubspotintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Assigns a sales rep owner.
 * Input: contactId, industry, companySize
 * Output: ownerId, ownerName
 */
public class AssignOwnerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hs_assign_owner";
    }

    @Override
    public TaskResult execute(Task task) {
        String contactId = (String) task.getInputData().get("contactId");
        String industry = (String) task.getInputData().get("industry");
        String companySize = (String) task.getInputData().get("companySize");
        System.out.println("  [assign] Contact " + contactId + " -> Sarah Johnson (" + industry + ", " + companySize + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ownerId", "rep-042");
        result.getOutputData().put("ownerName", "Sarah Johnson");
        return result;
    }
}
