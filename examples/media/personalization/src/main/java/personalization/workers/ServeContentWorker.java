package personalization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ServeContentWorker implements Worker {
    @Override public String getTaskDefName() { return "per_serve_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [serve] Processing " + task.getInputData().getOrDefault("servedCount", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("servedCount", "items.length");
        r.getOutputData().put("responseTimeMs", 45);
        r.getOutputData().put("cacheHit", false);
        r.getOutputData().put("experimentId", "EXP-519-A");
        return r;
    }
}
