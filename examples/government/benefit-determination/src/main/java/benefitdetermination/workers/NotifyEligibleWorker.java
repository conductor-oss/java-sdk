package benefitdetermination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyEligibleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bnd_notify_eligible";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        Object benefit = task.getInputData().get("benefit");
        System.out.printf("  [eligible] %s approved for $%s/month%n", applicantId, benefit);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("approved", true);
        return result;
    }
}
