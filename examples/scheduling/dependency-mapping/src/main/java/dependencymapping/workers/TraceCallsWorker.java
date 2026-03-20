package dependencymapping.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class TraceCallsWorker implements Worker {
    @Override public String getTaskDefName() { return "dep_trace_calls"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [trace] Tracing inter-service calls across " + task.getInputData().get("serviceCount") + " services");
        r.getOutputData().put("edgeCount", 14);
        r.getOutputData().put("edges", List.of(Map.of("from","api-gateway","to","auth-service")));
        return r;
    }
}
