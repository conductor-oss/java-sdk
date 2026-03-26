package customerjourney.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Tracks customer touchpoints across channels.
 * Input: customerId, timeWindow
 * Output: touchpoints (list), touchpointCount
 */
public class TrackTouchpointsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cjy_track_touchpoints";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        System.out.println("  [track] Tracked touchpoints for customer " + customerId);

        List<Map<String, String>> touchpoints = List.of(
                Map.of("channel", "website", "action", "visit", "timestamp", "2024-01-05"),
                Map.of("channel", "email", "action", "open", "timestamp", "2024-01-07"),
                Map.of("channel", "website", "action", "signup", "timestamp", "2024-01-08"),
                Map.of("channel", "app", "action", "purchase", "timestamp", "2024-01-15")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("touchpoints", touchpoints);
        result.getOutputData().put("touchpointCount", 14);
        return result;
    }
}
