package eventreplay.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays filtered events. Each event is replayed with a deterministic success status.
 * Returns replay results with replayStatus "success" and a fixed replayedAt timestamp.
 */
public class ReplayEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ep_replay_events";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> filteredEvents =
                (List<Map<String, Object>>) task.getInputData().get("filteredEvents");
        if (filteredEvents == null) {
            filteredEvents = List.of();
        }

        System.out.println("  [ep_replay_events] Replaying " + filteredEvents.size() + " events");

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        for (Map<String, Object> event : filteredEvents) {
            Map<String, Object> replayResult = new HashMap<>();
            replayResult.put("eventId", event.get("id"));
            replayResult.put("originalType", event.get("type"));
            replayResult.put("replayStatus", "success");
            replayResult.put("replayedAt", "2026-01-15T10:00:00Z");
            results.add(replayResult);
            successCount++;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("totalReplayed", filteredEvents.size());
        result.getOutputData().put("successCount", successCount);
        return result;
    }
}
