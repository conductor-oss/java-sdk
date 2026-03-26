package contentarchival.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IndexArchiveWorker implements Worker {
    @Override public String getTaskDefName() { return "car_index_archive"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [index] Processing " + task.getInputData().getOrDefault("indexId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("indexId", "IDX-528-2026");
        r.getOutputData().put("searchable", true);
        r.getOutputData().put("indexedAt", "2026-03-08T04:30:00Z");
        return r;
    }
}
