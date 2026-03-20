package metricscollection.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectAppWorker implements Worker {
    @Override public String getTaskDefName() { return "mc_collect_app"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [application] Collecting application metrics from " + task.getInputData().get("environment"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("metricCount", 45);
        r.getOutputData().put("source", "application");
        return r;
    }
}
