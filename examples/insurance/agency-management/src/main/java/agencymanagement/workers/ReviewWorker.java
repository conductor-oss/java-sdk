package agencymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agm_review";
    }

    @Override
    public TaskResult execute(Task task) {

        String agentId = (String) task.getInputData().get("agentId");
        System.out.printf("  [review] Agent %s rated: exceeds expectations%n", agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rating", "exceeds-expectations");
        result.getOutputData().put("bonusEligible", true);
        return result;
    }
}
