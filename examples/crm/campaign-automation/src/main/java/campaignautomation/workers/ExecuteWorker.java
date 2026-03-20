package campaignautomation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "cpa_execute"; }

    @Override
    public TaskResult execute(Task task) {
        String execId = "EXC-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        System.out.println("  [execute] Campaign launched -> " + execId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("executionId", execId);
        result.getOutputData().put("impressions", 340000);
        result.getOutputData().put("clicks", 8500);
        result.getOutputData().put("sentAt", Instant.now().toString());
        return result;
    }
}
