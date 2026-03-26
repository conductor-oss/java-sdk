package usermigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class VerifyMigrationWorker implements Worker {
    @Override public String getTaskDefName() { return "umg_verify"; }
    @Override public TaskResult execute(Task task) {
        Object loaded = task.getInputData().get("loaded");
        Object expected = task.getInputData().get("expected");
        boolean match = loaded != null && loaded.equals(expected);
        System.out.println("  [verify] Verification: " + (match ? "all records match" : "mismatch detected"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allMatch", match);
        result.getOutputData().put("verificationResult", Map.of("loaded", loaded, "expected", expected));
        return result;
    }
}
