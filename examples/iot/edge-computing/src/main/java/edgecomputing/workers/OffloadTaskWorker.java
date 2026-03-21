package edgecomputing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OffloadTaskWorker implements Worker {
    @Override public String getTaskDefName() { return "edg_offload_task"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [offload] Processing " + task.getInputData().getOrDefault("edgeJobId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("edgeJobId", "EDGE-JOB-534-001");
        r.getOutputData().put("scheduledAt", "2026-03-08T10:00:00Z");
        r.getOutputData().put("estimatedCompletionMs", 500);
        return r;
    }
}
