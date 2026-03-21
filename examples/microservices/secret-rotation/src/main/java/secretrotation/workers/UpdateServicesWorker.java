package secretrotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class UpdateServicesWorker implements Worker {
    @Override public String getTaskDefName() { return "sr_update_services"; }

    @Override @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object svcs = task.getInputData().get("targetServices");
        List<String> services = svcs instanceof List ? (List<String>) svcs : List.of("api", "worker", "scheduler");
        System.out.println("  [update] Updating " + services.size() + " services with new secret...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("updatedServices", services);
        result.getOutputData().put("updatedCount", services.size());
        result.getOutputData().put("restartRequired", false);
        return result;
    }
}
