package dataarchival.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Creates a snapshot of stale records.
 * Input: staleRecords (list)
 * Output: snapshot (map with timestamp, recordCount, records, sizeBytes)
 */
public class SnapshotRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "arc_snapshot_records";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("staleRecords");
        if (records == null) {
            records = List.of();
        }

        int sizeBytes = records.toString().length();
        Map<String, Object> snapshot = Map.of(
                "timestamp", Instant.now().toString(),
                "recordCount", records.size(),
                "records", records,
                "sizeBytes", sizeBytes
        );

        System.out.println("  [snapshot] Created snapshot: " + records.size() + " records, " + sizeBytes + " bytes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("snapshot", snapshot);
        return result;
    }
}
