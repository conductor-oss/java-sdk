package emailcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackEngagementWorker implements Worker {
    @Override public String getTaskDefName() { return "eml_track_engagement"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Processing " + task.getInputData().getOrDefault("openRate", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("openRate", 24.5);
        r.getOutputData().put("clickRate", 3.8);
        r.getOutputData().put("unsubscribeRate", 0.12);
        r.getOutputData().put("uniqueOpens", 5245);
        r.getOutputData().put("uniqueClicks", 570);
        return r;
    }
}
