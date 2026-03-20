package gdprdatadeletion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Finds all records associated with a user across multiple systems.
 * Input: userId
 * Output: records (list), recordCount, systems (list of system names)
 */
public class FindRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gr_find_records";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().getOrDefault("userId", "unknown");

        List<Map<String, Object>> records = List.of(
                Map.of("system", "user_accounts", "table", "users", "recordId", "USR-001", "dataTypes", List.of("profile", "credentials")),
                Map.of("system", "user_accounts", "table", "preferences", "recordId", "PREF-001", "dataTypes", List.of("settings")),
                Map.of("system", "analytics", "table", "user_events", "recordId", "EVT-1001", "dataTypes", List.of("clickstream", "sessions")),
                Map.of("system", "analytics", "table", "user_events", "recordId", "EVT-1002", "dataTypes", List.of("clickstream")),
                Map.of("system", "billing", "table", "invoices", "recordId", "INV-2001", "dataTypes", List.of("payment_info", "address")),
                Map.of("system", "support", "table", "tickets", "recordId", "TKT-301", "dataTypes", List.of("messages", "attachments")),
                Map.of("system", "marketing", "table", "email_list", "recordId", "MKT-401", "dataTypes", List.of("email", "preferences"))
        );

        Set<String> systemSet = new LinkedHashSet<>();
        for (Map<String, Object> r : records) {
            systemSet.add((String) r.get("system"));
        }
        List<String> systems = new ArrayList<>(systemSet);

        System.out.println("  [find] Found " + records.size() + " records for user " + userId
                + " across " + systems.size() + " systems: " + String.join(", ", systems));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("recordCount", records.size());
        result.getOutputData().put("systems", systems);
        return result;
    }
}
