package regulatoryfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "rgf_submit"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Submitting regulatory filing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submissionId", "SUB-" + System.currentTimeMillis());
        result.getOutputData().put("submittedAt", java.time.Instant.now().toString());
        return result;
    }
}
