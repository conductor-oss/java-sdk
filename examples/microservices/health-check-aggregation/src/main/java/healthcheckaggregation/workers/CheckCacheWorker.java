package healthcheckaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckCacheWorker implements Worker {
    @Override public String getTaskDefName() { return "hc_check_cache"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cache] Redis: healthy (memory: 45%)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("healthy", true);
        r.getOutputData().put("memoryPct", 45);
        r.getOutputData().put("component", "redis");
        return r;
    }
}
