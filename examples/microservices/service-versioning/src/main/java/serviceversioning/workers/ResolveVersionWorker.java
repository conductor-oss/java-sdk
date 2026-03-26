package serviceversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ResolveVersionWorker implements Worker {
    @Override public String getTaskDefName() { return "sv_resolve_version"; }
    @Override public TaskResult execute(Task task) {
        String apiVersion = (String) task.getInputData().getOrDefault("apiVersion", "v1");
        String resolved = "v2".equals(apiVersion) ? "v2" : "v1";
        System.out.println("  [version] API " + apiVersion + " -> " + resolved);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("resolvedVersion", resolved);
        r.getOutputData().put("deprecated", "v1".equals(resolved));
        return r;
    }
}
