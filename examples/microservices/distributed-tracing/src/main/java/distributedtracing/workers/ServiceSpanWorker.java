package distributedtracing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ServiceSpanWorker implements Worker {
    @Override public String getTaskDefName() { return "dt_service_span"; }
    @Override public TaskResult execute(Task task) {
        String parentSpanId = (String) task.getInputData().getOrDefault("parentSpanId", "unknown");
        System.out.println("  [span] Service span under " + parentSpanId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("spanId", "span-svc-" + System.currentTimeMillis());
        r.getOutputData().put("durationMs", 45);
        return r;
    }
}
