package distributedtracing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CreateTraceWorker implements Worker {
    @Override public String getTaskDefName() { return "dt_create_trace"; }
    @Override public TaskResult execute(Task task) {
        String traceId = "trace-" + System.currentTimeMillis();
        String operation = (String) task.getInputData().getOrDefault("operation", "unknown");
        System.out.println("  [trace] Created: " + traceId + " for " + operation);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("traceId", traceId);
        r.getOutputData().put("spanId", "span-root-" + System.currentTimeMillis());
        return r;
    }
}
