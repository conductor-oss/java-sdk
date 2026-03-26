package dripcampaign.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackEngagementWorker implements Worker {
    @Override public String getTaskDefName() { return "drp_track_engagement"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [track] Engagement tracked: 4/5 opened, 2/5 clicked");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("engagementScore", 78);
        result.getOutputData().put("opens", 4);
        result.getOutputData().put("clicks", 2);
        result.getOutputData().put("replies", 1);
        return result;
    }
}
