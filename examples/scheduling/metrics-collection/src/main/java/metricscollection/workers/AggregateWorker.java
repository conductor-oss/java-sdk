package metricscollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AggregateWorker implements Worker {
    @Override public String getTaskDefName() { return "mc_aggregate"; }
    @Override public TaskResult execute(Task task) {
        int app = 0, infra = 0, biz = 0;
        try { app = Integer.parseInt(String.valueOf(task.getInputData().get("appMetrics"))); } catch (Exception ignored) {}
        try { infra = Integer.parseInt(String.valueOf(task.getInputData().get("infraMetrics"))); } catch (Exception ignored) {}
        try { biz = Integer.parseInt(String.valueOf(task.getInputData().get("bizMetrics"))); } catch (Exception ignored) {}
        int total = app + infra + biz;
        System.out.println("  [aggregate] Aggregating " + total + " metrics from 3 sources");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("totalMetrics", total);
        r.getOutputData().put("sources", 3);
        return r;
    }
}
