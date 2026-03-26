package calendarintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class NotifyStakeholdersWorker implements Worker {
    @Override public String getTaskDefName() { return "cal_notify_stakeholders"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [notify] Notifying stakeholders of " + task.getInputData().get("changesSynced") + " synced changes");
        result.getOutputData().put("notified", true);
        result.getOutputData().put("recipientCount", 8);
        return result;
    }
}
