package contentsyndication.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackPerformanceWorker implements Worker {
    @Override public String getTaskDefName() { return "syn_track_performance"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Processing " + task.getInputData().getOrDefault("trackingId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("trackingId", "TRK-523-001");
        r.getOutputData().put("trackingPixels", "platforms.length");
        r.getOutputData().put("utmCampaign", "syndication-523");
        return r;
    }
}
