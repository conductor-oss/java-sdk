package custommetrics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CusAggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "cus_aggregate"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [aggregate] Aggregating " + task.getInputData().get("rawDataPoints") + " data points");
        r.getOutputData().put("metricCount", 4);
        r.getOutputData().put("aggregatedMetrics", List.of(Map.of("name","order_processing_time","p50",1200)));
        return r;
    }
}
