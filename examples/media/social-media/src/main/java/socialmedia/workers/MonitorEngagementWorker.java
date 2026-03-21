package socialmedia.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MonitorEngagementWorker implements Worker {
    @Override public String getTaskDefName() { return "soc_monitor_engagement"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Processing " + task.getInputData().getOrDefault("impressions", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("impressions", 12500);
        r.getOutputData().put("likes", 340);
        r.getOutputData().put("shares", 85);
        r.getOutputData().put("commentsCount", 42);
        r.getOutputData().put("mentionsCount", 7);
        r.getOutputData().put("engagementRate", 3.74);
        return r;
    }
}
