package trademarksearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessWorker implements Worker {
    @Override public String getTaskDefName() { return "tmk_assess"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [assess] Assessing trademark risk");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assessment", "moderate-risk");
        result.getOutputData().put("riskLevel", "moderate");
        return result;
    }
}
