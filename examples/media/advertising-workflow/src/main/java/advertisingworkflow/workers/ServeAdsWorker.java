package advertisingworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ServeAdsWorker implements Worker {
    @Override public String getTaskDefName() { return "adv_serve_ads"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [serve] Processing " + task.getInputData().getOrDefault("impressions", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("impressions", 850000);
        r.getOutputData().put("clicks", 12750);
        r.getOutputData().put("conversions", 425);
        r.getOutputData().put("spend", 8500);
        r.getOutputData().put("ctr", 1.5);
        return r;
    }
}
