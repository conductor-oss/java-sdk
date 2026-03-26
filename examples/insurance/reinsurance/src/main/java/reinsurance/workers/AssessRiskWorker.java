package reinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessRiskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rin_assess_risk";
    }

    @Override
    public TaskResult execute(Task task) {

        String policyId = (String) task.getInputData().get("policyId");
        System.out.printf("  [assess] Policy %s exposure assessed%n", policyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("netExposure", 5000000);
        result.getOutputData().put("retainedRisk", 1500000);
        return result;
    }
}
