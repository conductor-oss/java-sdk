package custommetrics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DefineMetricsWorker implements Worker {
    @Override public String getTaskDefName() { return "cus_define_metrics"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [define] Registering custom metric definitions");
        r.getOutputData().put("registeredMetrics", 4);
        return r;
    }
}
