package citizenrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctz_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String citizenId = (String) task.getInputData().get("citizenId");
        String resolution = (String) task.getInputData().get("resolution");
        System.out.printf("  [notify] Citizen %s notified: %s%n", citizenId, resolution);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        return result;
    }
}
