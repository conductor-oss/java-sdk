package performancereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfr_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        String employeeId = (String) task.getInputData().get("employeeId");
        Object calibratedRating = task.getInputData().get("calibratedRating");

        System.out.println("  [finalize] Review finalized for " + employeeId + ": " + calibratedRating);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reviewId", "REV-608");
        result.getOutputData().put("finalized", true);
        result.getOutputData().put("meritIncrease", "5%");
        return result;
    }
}
