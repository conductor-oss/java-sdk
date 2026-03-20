package apmworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AnalyzeLatencyWorker implements Worker {
    @Override public String getTaskDefName() { return "apm_analyze_latency"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [latency] Analyzing latency");
        r.getOutputData().put("p50Latency", 45);
        r.getOutputData().put("p95Latency", 180);
        r.getOutputData().put("p99Latency", 520);
        return r;
    }
}
