package logaggregation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class EnrichLogsWorker implements Worker {
    @Override public String getTaskDefName() { return "la_enrich_logs"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [enrich] Enriching " + task.getInputData().get("parsedCount") + " parsed logs with metadata");
        r.getOutputData().put("enrichedCount", 14800);
        r.getOutputData().put("sizeBytes", 45000000);
        r.getOutputData().put("fieldsAdded", List.of("geo", "service", "traceId", "userId"));
        return r;
    }
}
