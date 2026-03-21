package litigationhold.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "lth_notify"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [notify] Sending hold notices for case " + task.getInputData().get("caseId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notifiedCount", 5);
        result.getOutputData().put("notificationsSent", true);
        return result;
    }
}
