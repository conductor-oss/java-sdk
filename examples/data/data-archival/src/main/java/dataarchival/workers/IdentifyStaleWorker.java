package dataarchival.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Identifies stale records based on retention days.
 * Input: records (list), retentionDays (int)
 * Output: staleRecords (list), staleIds (list), staleCount (int), totalCount (int)
 */
public class IdentifyStaleWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "arc_identify_stale";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }
        Object retObj = task.getInputData().get("retentionDays");
        int retentionDays = retObj instanceof Number ? ((Number) retObj).intValue() : 90;

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        List<Map<String, Object>> stale = new ArrayList<>();
        List<Map<String, Object>> fresh = new ArrayList<>();
        for (Map<String, Object> record : records) {
            String dateStr = record.get("lastAccessed") != null
                    ? record.get("lastAccessed").toString()
                    : (record.get("createdAt") != null ? record.get("createdAt").toString() : null);
            try {
                Instant recordDate = dateStr != null ? Instant.parse(dateStr) : Instant.now();
                if (recordDate.isBefore(cutoff)) {
                    stale.add(record);
                } else {
                    fresh.add(record);
                }
            } catch (Exception e) {
                stale.add(record);
            }
        }

        List<Object> staleIds = stale.stream().map(r -> r.get("id")).toList();

        System.out.println("  [identify] " + stale.size() + " stale records (older than " + retentionDays + " days), " + fresh.size() + " fresh");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("staleRecords", stale);
        result.getOutputData().put("staleIds", staleIds);
        result.getOutputData().put("staleCount", stale.size());
        result.getOutputData().put("totalCount", records.size());
        return result;
    }
}
