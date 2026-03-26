package distributedtracing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExportTraceWorker implements Worker {
    @Override public String getTaskDefName() { return "dt_export_trace"; }
    @Override public TaskResult execute(Task task) {
        Object spanCount = task.getInputData().getOrDefault("spanCount", 0);
        System.out.println("  [export] Exported " + spanCount + " spans to Jaeger");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("exported", true);
        r.getOutputData().put("destination", "jaeger");
        return r;
    }
}
