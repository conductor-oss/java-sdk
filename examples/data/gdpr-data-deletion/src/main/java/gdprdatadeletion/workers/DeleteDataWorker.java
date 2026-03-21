package gdprdatadeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Deletes user data from all systems if identity is verified.
 * Input: userId, records (list), verified (boolean)
 * Output: deletedCount, deletedRecords, timestamp
 */
public class DeleteDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gr_delete_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("records", List.of());
        Object verifiedObj = task.getInputData().get("verified");
        boolean verified = Boolean.TRUE.equals(verifiedObj);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (!verified) {
            System.out.println("  [delete] ABORTED - identity not verified");
            result.getOutputData().put("deletedCount", 0);
            result.getOutputData().put("deletedRecords", List.of());
            result.getOutputData().put("timestamp", null);
            return result;
        }

        List<Map<String, Object>> deleted = new ArrayList<>();
        Set<String> systems = new LinkedHashSet<>();
        for (Map<String, Object> r : records) {
            Map<String, Object> del = new LinkedHashMap<>(r);
            del.put("status", "deleted");
            del.put("deletedAt", "2024-03-15T14:30:00Z");
            deleted.add(del);
            systems.add((String) r.get("system"));
        }

        System.out.println("  [delete] Deleted " + deleted.size() + " records from " + systems.size() + " systems");

        result.getOutputData().put("deletedCount", deleted.size());
        result.getOutputData().put("deletedRecords", deleted);
        result.getOutputData().put("timestamp", "2024-03-15T14:30:00Z");
        return result;
    }
}
