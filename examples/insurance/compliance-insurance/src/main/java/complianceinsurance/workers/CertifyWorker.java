package complianceinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CertifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cpi_certify";
    }

    @Override
    public TaskResult execute(Task task) {

        String companyId = (String) task.getInputData().get("companyId");
        System.out.printf("  [certify] %s certified compliant%n", companyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("complianceStatus", "compliant");
        result.getOutputData().put("certificationId", "CERT-compliance-insurance-001");
        return result;
    }
}
