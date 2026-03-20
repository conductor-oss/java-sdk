package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Takes a snapshot of the current aggregate state for fast future hydration.
 * Returns a fixed snapshot ID and timestamp.
 */
public class SnapshotStateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ev_snapshot_state";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String aggregateId = (String) task.getInputData().get("aggregateId");
        Map<String, Object> currentState =
                (Map<String, Object>) task.getInputData().get("currentState");
        int version = task.getInputData().get("version") instanceof Number
                ? ((Number) task.getInputData().get("version")).intValue()
                : 0;

        if (aggregateId == null || aggregateId.isBlank()) {
            aggregateId = "unknown";
        }

        System.out.println("  [ev_snapshot_state] Snapshotting " + aggregateId
                + " at version " + version);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("snapshotId", "snap-fixed-001");
        result.getOutputData().put("snapshotAt", "2026-01-15T10:00:00Z");
        result.getOutputData().put("version", version);
        result.getOutputData().put("aggregateId", aggregateId);
        result.getOutputData().put("state", currentState);
        return result;
    }
}
