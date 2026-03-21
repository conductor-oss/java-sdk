package insurancerenewal.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "irn_review";
    }

    @Override
    public TaskResult execute(Task task) {

        String policyId = (String) task.getInputData().get("policyId");
        System.out.printf("  [review] Policy %s reviewed — 1 claim in history%n", policyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskScore", 0.35);
        result.getOutputData().put("claimsCount", 1);
        return result;
    }
}
