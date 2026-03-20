package usermigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyMigrationWorker implements Worker {
    @Override public String getTaskDefName() { return "umg_notify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [notify] Migration completion notification sent");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("channel", "slack");
        return result;
    }
}
