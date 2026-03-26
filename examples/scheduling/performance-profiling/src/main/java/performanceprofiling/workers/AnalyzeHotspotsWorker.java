package performanceprofiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AnalyzeHotspotsWorker implements Worker {
    @Override public String getTaskDefName() { return "prf_analyze_hotspots"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [analyze] Analyzing " + task.getInputData().get("sampleCount") + " samples for hotspots");
        r.getOutputData().put("hotspotCount", 5);
        r.getOutputData().put("topHotspot", "JSONSerializer.serialize() - 22% CPU");
        r.getOutputData().put("hotspots", List.of(Map.of("function","JSONSerializer.serialize()","cpuPercent",22)));
        return r;
    }
}
