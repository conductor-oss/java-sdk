package distributedtracing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DbSpanWorker implements Worker {
    @Override public String getTaskDefName() { return "dt_db_span"; }
    @Override public TaskResult execute(Task task) {
        String parentSpanId = (String) task.getInputData().getOrDefault("parentSpanId", "unknown");
        System.out.println("  [span] DB span under " + parentSpanId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("spanId", "span-db-" + System.currentTimeMillis());
        r.getOutputData().put("durationMs", 12);
        return r;
    }
}
