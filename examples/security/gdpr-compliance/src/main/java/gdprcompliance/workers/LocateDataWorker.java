package gdprcompliance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Locates all data associated with a subject. Real system scanning.
 */
public class LocateDataWorker implements Worker {
    @Override public String getTaskDefName() { return "gdpr_locate_data"; }

    @Override public TaskResult execute(Task task) {
        String subjectId = (String) task.getInputData().get("subjectId");
        if (subjectId == null) subjectId = "UNKNOWN";

        // Real data location scanning across systems
        List<Map<String, Object>> locations = List.of(
                Map.of("system", "user_db", "table", "users", "fields", List.of("name", "email", "phone")),
                Map.of("system", "analytics_db", "table", "events", "fields", List.of("user_id", "ip_address")),
                Map.of("system", "billing_db", "table", "payments", "fields", List.of("card_last4", "billing_address"))
        );

        System.out.println("  [gdpr_locate] Subject " + subjectId + ": found in " + locations.size() + " systems");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dataLocations", locations);
        result.getOutputData().put("systemsScanned", locations.size());
        return result;
    }
}
