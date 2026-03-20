package rollingupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analyzes the current deployment state before a rolling update.
 */
public class Analyze implements Worker {

    @Override
    public String getTaskDefName() {
        return "ru_analyze";
    }

    @Override
    public TaskResult execute(Task task) {
        String service = (String) task.getInputData().get("service");
        String newVersion = (String) task.getInputData().get("newVersion");

        if (service == null) service = "unknown-service";
        if (newVersion == null) newVersion = "0.0.0";

        System.out.println("[ru_analyze] " + service + ": analyzing for update to " + newVersion);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("analyzeId", "ANALYZE-1342");
        output.put("success", true);
        output.put("service", service);
        output.put("newVersion", newVersion);

        int replicas;
        String currentVersion;
        switch (service) {
            case "user-service":    replicas = 5; currentVersion = "2.4.0"; break;
            case "order-service":   replicas = 3; currentVersion = "1.8.2"; break;
            case "payment-service": replicas = 4; currentVersion = "3.1.0"; break;
            default:                replicas = 2; currentVersion = "1.0.0"; break;
        }

        output.put("currentReplicas", replicas);
        output.put("currentVersion", currentVersion);
        output.put("healthyReplicas", replicas);

        System.out.println("[ru_analyze] " + service + ": " + replicas + " replicas running v" + currentVersion);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
