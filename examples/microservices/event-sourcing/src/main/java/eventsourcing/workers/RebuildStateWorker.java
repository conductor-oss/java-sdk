package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class RebuildStateWorker implements Worker {
    @Override public String getTaskDefName() { return "es_rebuild_state"; }
    @Override public TaskResult execute(Task task) {
        Object version = task.getInputData().getOrDefault("latestVersion", 0);
        System.out.println("  [rebuild] State rebuilt from " + version + " events");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("state", Map.of("balance", 1500, "status", "active"));
        return r;
    }
}
