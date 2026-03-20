package emailcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class AnalyzeResultsWorker implements Worker {
    @Override public String getTaskDefName() { return "eml_analyze_results"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Processing " + task.getInputData().getOrDefault("industryBenchmark", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("industryBenchmark", Map.of());
        r.getOutputData().put("clickRate", 2.6);
        return r;
    }
}
