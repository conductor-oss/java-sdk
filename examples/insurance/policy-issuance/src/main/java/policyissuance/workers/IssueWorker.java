package policyissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IssueWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pis_issue";
    }

    @Override
    public TaskResult execute(Task task) {

        String policyId = (String) task.getInputData().get("policyId");
        System.out.printf("  [issue] Policy %s officially issued%n", policyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("issued", true);
        result.getOutputData().put("effectiveDate", "2024-04-01");
        return result;
    }
}
