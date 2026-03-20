package metricscollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectInfraWorker implements Worker {
    @Override public String getTaskDefName() { return "mc_collect_infra"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [infrastructure] Collecting infrastructure metrics from " + task.getInputData().get("environment"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metricCount", 32);
        r.getOutputData().put("source", "infrastructure");
        return r;
    }
}
