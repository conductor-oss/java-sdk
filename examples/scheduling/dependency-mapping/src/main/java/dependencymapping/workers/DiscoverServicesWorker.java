package dependencymapping.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DiscoverServicesWorker implements Worker {
    @Override public String getTaskDefName() { return "dep_discover_services"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [discover] Discovering services in " + task.getInputData().get("environment"));
        r.getOutputData().put("serviceCount", 8);
        r.getOutputData().put("services", List.of("api-gateway","auth-service","user-service","order-service"));
        return r;
    }
}
