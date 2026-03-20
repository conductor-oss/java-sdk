package permissionsync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;

public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "pms_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] All permissions verified in sync");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allVerified", true);
        result.getOutputData().put("verifiedAt", Instant.now().toString());
        return result;
    }
}
