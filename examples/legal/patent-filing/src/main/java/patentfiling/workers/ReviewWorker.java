package patentfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "ptf_review"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [review] Reviewing draft " + task.getInputData().get("draftId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reviewStatus", "approved");
        result.getOutputData().put("comments", "Claims well-drafted");
        return result;
    }
}
