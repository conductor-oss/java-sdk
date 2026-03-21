package documentqa.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class IngestWorker implements Worker {
    @Override public String getTaskDefName() { return "dqa_ingest"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [ingest] Document ingested from " + task.getInputData().get("documentUrl"));r.getOutputData().put("document", java.util.Map.of("title", "Q4 Earnings Report", "pages", 24, "wordCount", 8500, "format", "pdf"));
        return r;
    }
}
