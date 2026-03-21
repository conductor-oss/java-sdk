package serviceactivation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ActivateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sac_activate";
    }

    @Override
    public TaskResult execute(Task task) {

        String serviceId = (String) task.getInputData().get("serviceId");
        System.out.printf("  [activate] Service %s activated%n", serviceId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("activated", true);
        result.getOutputData().put("activatedAt", "2024-03-10T12:00:00Z");
        return result;
    }
}
