package apmworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectTracesWorker implements Worker {
    @Override public String getTaskDefName() { return "apm_collect_traces"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Collecting traces for " + task.getInputData().get("serviceName"));
        r.getOutputData().put("traceCount", 25000);
        return r;
    }
}
