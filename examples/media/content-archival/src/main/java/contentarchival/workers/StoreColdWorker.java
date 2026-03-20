package contentarchival.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class StoreColdWorker implements Worker {
    @Override public String getTaskDefName() { return "car_store_cold"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [store] Processing " + task.getInputData().getOrDefault("coldStoragePath", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("coldStoragePath", "glacier://archive/528/2026-03-08");
        r.getOutputData().put("storageClass", "GLACIER_DEEP_ARCHIVE");
        r.getOutputData().put("retrievalTime", "12-48 hours");
        r.getOutputData().put("storedAt", "2026-03-08T04:00:00Z");
        return r;
    }
}
