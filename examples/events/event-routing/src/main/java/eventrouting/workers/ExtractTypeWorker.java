package eventrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Splits the eventDomain string by "." to extract the domain (first part)
 * and subType (remaining parts joined by ".").
 * Example: "user.profile_update" -> domain="user", subType="profile_update"
 */
public class ExtractTypeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eo_extract_type";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventDomain = (String) task.getInputData().get("eventDomain");
        if (eventDomain == null || eventDomain.isBlank()) {
            eventDomain = "unknown";
        }

        System.out.println("  [eo_extract_type] Extracting type from: " + eventDomain);

        String domain;
        String subType;

        int dotIndex = eventDomain.indexOf('.');
        if (dotIndex >= 0) {
            domain = eventDomain.substring(0, dotIndex);
            subType = eventDomain.substring(dotIndex + 1);
        } else {
            domain = eventDomain;
            subType = "";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("domain", domain);
        result.getOutputData().put("subType", subType);
        return result;
    }
}
