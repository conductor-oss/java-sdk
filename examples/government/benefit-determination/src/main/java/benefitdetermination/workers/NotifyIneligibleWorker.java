package benefitdetermination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyIneligibleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bnd_notify_ineligible";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        String reason = (String) task.getInputData().get("reason");
        System.out.printf("  [ineligible] %s denied: %s%n", applicantId, reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("approved", false);
        return result;
    }
}
