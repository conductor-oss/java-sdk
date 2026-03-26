package governmentpermit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gvp_review";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [review] Zoning board review complete — approved");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", "approve");
        result.getOutputData().put("reason", null);
        return result;
    }
}
