package circuitbreakermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class FallbackWorker implements Worker {
    @Override public String getTaskDefName() { return "cb_fallback"; }
    @Override public TaskResult execute(Task task) {
        String svc = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        System.out.println("  [fallback] Circuit open for " + svc + ", returning cached data");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("response", Map.of("data", "cached_response"));
        r.getOutputData().put("fromCache", true);
        return r;
    }
}
