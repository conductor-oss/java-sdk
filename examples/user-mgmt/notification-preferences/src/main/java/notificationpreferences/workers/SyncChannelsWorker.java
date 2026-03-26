package notificationpreferences.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Syncs active notification channels.
 * Input: userId, updated
 * Output: syncedChannels, syncedAt
 */
public class SyncChannelsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "np_sync_channels";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> prefs = (Map<String, Object>) task.getInputData().get("updated");
        List<String> active = new ArrayList<>();
        if (prefs != null) {
            for (Map.Entry<String, Object> entry : prefs.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    active.add(entry.getKey());
                }
            }
        }

        System.out.println("  [sync] Synced " + active.size() + " active channels: " + String.join(", ", active));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("syncedChannels", active);
        result.getOutputData().put("syncedAt", Instant.now().toString());
        return result;
    }
}
