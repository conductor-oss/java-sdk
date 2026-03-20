package premiumcalculation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectFactorsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pmc_collect_factors";
    }

    @Override
    public TaskResult execute(Task task) {

        String policyType = (String) task.getInputData().get("policyType");
        System.out.printf("  [collect] Factors gathered for %s policy%n", policyType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("factors", java.util.Map.of("age", 35, "drivingRecord", "clean", "creditScore", 720));
        return result;
    }
}
