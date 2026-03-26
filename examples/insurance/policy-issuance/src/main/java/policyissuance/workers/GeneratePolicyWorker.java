package policyissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GeneratePolicyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pis_generate_policy";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [generate] Policy document generated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("policyId", "POL-policy-issuance-001");
        result.getOutputData().put("documentUrl", "/policies/POL-policy-issuance-001.pdf");
        return result;
    }
}
