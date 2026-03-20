package taxassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "txa_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String ownerId = (String) task.getInputData().get("ownerId");
        Object taxAmount = task.getInputData().get("taxAmount");
        System.out.printf("  [notify] Owner %s notified of tax: $%s%n", ownerId, taxAmount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        return result;
    }
}
