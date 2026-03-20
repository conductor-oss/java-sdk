package wiretap.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WtpTapAuditWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wtp_tap_audit";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [audit] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String auditLogId = "AUD-" + Long.toString(System.currentTimeMillis(), 36);
        result.getOutputData().put("tapped", true);
        result.getOutputData().put("auditLogId", auditLogId);
        result.getOutputData().put("auditEntry", java.util.Map.of("timestamp", java.time.Instant.now().toString(), "level", task.getInputData().getOrDefault("auditLevel", "info"), "source", "wire_tap"));
        return result;
    }
}