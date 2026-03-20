package metricscollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectBusinessWorker implements Worker {
    @Override public String getTaskDefName() { return "mc_collect_business"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [business] Collecting business metrics from " + task.getInputData().get("environment"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metricCount", 18);
        r.getOutputData().put("source", "business");
        return r;
    }
}
