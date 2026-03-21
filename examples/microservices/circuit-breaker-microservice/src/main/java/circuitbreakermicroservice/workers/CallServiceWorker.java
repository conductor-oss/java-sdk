package circuitbreakermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CallServiceWorker implements Worker {
    @Override public String getTaskDefName() { return "cb_call_service"; }
    @Override public TaskResult execute(Task task) {
        String svc = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        System.out.println("  [call] Calling " + svc + "...");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("success", true);
        r.getOutputData().put("response", Map.of("data", "service_response"));
        r.getOutputData().put("latencyMs", 45);
        return r;
    }
}
