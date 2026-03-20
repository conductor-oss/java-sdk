package logaggregation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CollectLogsWorker implements Worker {
    @Override public String getTaskDefName() { return "la_collect_logs"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Collecting logs from sources (" + task.getInputData().get("timeRange") + ")");
        r.getOutputData().put("rawLogCount", 15000);
        r.getOutputData().put("format", "mixed");
        return r;
    }
}
