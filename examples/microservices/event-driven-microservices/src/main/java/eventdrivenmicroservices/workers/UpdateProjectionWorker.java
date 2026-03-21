package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateProjectionWorker implements Worker {
    @Override public String getTaskDefName() { return "edm_update_projection"; }
    @Override public TaskResult execute(Task task) {
        String processedData = (String) task.getInputData().getOrDefault("processedData", "unknown");
        System.out.println("  [projection] Updated read model with " + processedData);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("projectionUpdated", true);
        return r;
    }
}
