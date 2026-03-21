package remotemonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.*;

public class AlertActionWorker implements Worker {

    @Override
    public String getTaskDefName() { return "rpm_alert_action"; }

    @Override
    public TaskResult execute(Task task) {
        List<?> alerts = (List<?>) task.getInputData().getOrDefault("alerts", List.of());
        System.out.println("  [ALERT] " + alerts.size() + " alert(s) for " + task.getInputData().get("patientId") + ": " + alerts);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("action", "provider_notified");
        output.put("urgency", "high");
        output.put("notifiedProvider", "Dr. Martinez");
        output.put("notifiedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
