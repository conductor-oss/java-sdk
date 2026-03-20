package logaggregation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ParseLogsWorker implements Worker {
    @Override public String getTaskDefName() { return "la_parse_logs"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [parse] Parsing " + task.getInputData().get("rawLogCount") + " raw logs");
        r.getOutputData().put("parsedCount", 14800);
        r.getOutputData().put("parseErrors", 200);
        r.getOutputData().put("structuredLogs", "json");
        return r;
    }
}
