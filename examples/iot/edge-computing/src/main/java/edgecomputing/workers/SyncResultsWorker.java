package edgecomputing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SyncResultsWorker implements Worker {
    @Override public String getTaskDefName() { return "edg_sync_results"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [sync] Processing " + task.getInputData().getOrDefault("syncedData", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("syncedData", task.getInputData().get("edgeResult"));
        r.getOutputData().put("syncedAt", "2026-03-08T10:00:00.150Z");
        r.getOutputData().put("latencyMs", 105);
        r.getOutputData().put("dataSizeKb", 2.4);
        return r;
    }
}
