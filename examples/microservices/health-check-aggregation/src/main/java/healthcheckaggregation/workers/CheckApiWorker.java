package healthcheckaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckApiWorker implements Worker {
    @Override public String getTaskDefName() { return "hc_check_api"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [api] API gateway: healthy (latency: 15ms)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("healthy", true);
        r.getOutputData().put("latencyMs", 15);
        r.getOutputData().put("component", "api-gateway");
        return r;
    }
}
