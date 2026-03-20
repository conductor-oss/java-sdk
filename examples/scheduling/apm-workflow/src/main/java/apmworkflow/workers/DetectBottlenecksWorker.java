package apmworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DetectBottlenecksWorker implements Worker {
    @Override public String getTaskDefName() { return "apm_detect_bottlenecks"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [bottleneck] Detecting bottlenecks");
        r.getOutputData().put("bottleneckCount", 2);
        r.getOutputData().put("bottlenecks", List.of(Map.of("endpoint","/api/search","cause","N+1 query")));
        return r;
    }
}
