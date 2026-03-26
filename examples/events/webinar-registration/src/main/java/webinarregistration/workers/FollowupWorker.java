package webinarregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class FollowupWorker implements Worker {
    @Override public String getTaskDefName() { return "wbr_followup"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [followup] Post-webinar follow-up sent with recording link");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("content", Map.of(
                "recording", "https://webinar.example.com/recording/abc",
                "slides", "https://webinar.example.com/slides/abc.pdf"));
        return result;
    }
}
