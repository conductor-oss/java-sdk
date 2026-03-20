package performanceprofiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class RecommendWorker implements Worker {
    @Override public String getTaskDefName() { return "prf_recommend"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [recommend] Generating " + task.getInputData().get("hotspotCount") + " optimization recommendations");
        r.getOutputData().put("recommendationCount", 4);
        r.getOutputData().put("reportUrl", "https://perf.example.com/profile/prf-inst-20260308");
        r.getOutputData().put("estimatedImprovement", "35% CPU reduction");
        r.getOutputData().put("recommendations", List.of("Use streaming JSON serialization","Increase connection pool size"));
        return r;
    }
}
