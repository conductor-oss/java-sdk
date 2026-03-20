package reinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TreatyLookupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rin_treaty_lookup";
    }

    @Override
    public TaskResult execute(Task task) {

        String riskCategory = (String) task.getInputData().get("riskCategory");
        System.out.printf("  [treaty] Found quota-share treaty for %s%n", riskCategory);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("treatyId", "TRT-reinsurance-QS");
        result.getOutputData().put("reinsurer", "GlobalRe");
        result.getOutputData().put("cessionAmount", 5000000);
        return result;
    }
}
