package custommetrics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "cus_collect_data"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Collecting data for " + task.getInputData().get("registeredMetrics") + " metrics");
        r.getOutputData().put("rawDataPoints", 4800);
        return r;
    }
}
