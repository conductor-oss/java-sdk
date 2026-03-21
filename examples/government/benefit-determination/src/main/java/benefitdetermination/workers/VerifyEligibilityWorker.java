package benefitdetermination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyEligibilityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bnd_verify_eligibility";
    }

    @Override
    public TaskResult execute(Task task) {
        double income = 0;
        Object incomeObj = task.getInputData().get("income");
        if (incomeObj instanceof Number) {
            income = ((Number) incomeObj).doubleValue();
        }
        boolean eligible = income < 50000;
        System.out.printf("  [verify] Income $%.0f — %s%n", income, eligible ? "eligible" : "ineligible");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("eligibility", eligible ? "eligible" : "ineligible");
        result.getOutputData().put("reason", eligible ? null : "Income exceeds threshold");
        return result;
    }
}
