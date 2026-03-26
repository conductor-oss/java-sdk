package governmentpermit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IssueWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gvp_issue";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        System.out.printf("  [issue] Permit issued to %s%n", applicantId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("permitNumber", "GOV-2024-government-permit");
        result.getOutputData().put("issued", true);
        return result;
    }
}
