package logaggregation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class StoreLogsWorker implements Worker {
    @Override public String getTaskDefName() { return "la_store_logs"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [store] Storing " + task.getInputData().get("enrichedCount") + " enriched logs");
        r.getOutputData().put("stored", true);
        r.getOutputData().put("index", "logs-2026.03.08");
        r.getOutputData().put("documentsWritten", 14800);
        return r;
    }
}
