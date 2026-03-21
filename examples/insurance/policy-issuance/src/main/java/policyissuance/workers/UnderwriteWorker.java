package policyissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UnderwriteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pis_underwrite";
    }

    @Override
    public TaskResult execute(Task task) {

        String applicantId = (String) task.getInputData().get("applicantId");
        System.out.printf("  [underwrite] Applicant %s underwritten%n", applicantId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "approved");
        result.getOutputData().put("riskClass", "standard");
        return result;
    }
}
