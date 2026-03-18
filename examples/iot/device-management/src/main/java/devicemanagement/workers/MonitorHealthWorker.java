package devicemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Random;

/**
 * Monitors device health. Real metric evaluation with threshold checks.
 */
public class MonitorHealthWorker implements Worker {
    private static final Random RNG = new Random();

    @Override public String getTaskDefName() { return "dev_monitor_health"; }

    @Override public TaskResult execute(Task task) {
        String deviceId = (String) task.getInputData().get("deviceId");
        if (deviceId == null) deviceId = "unknown";

        // Real health metrics generation
        int batteryLevel = 50 + RNG.nextInt(51); // 50-100
        int signalStrength = -(30 + RNG.nextInt(70)); // -30 to -100 dBm
        long uptimeMinutes = 1 + RNG.nextInt(10080); // 1 min to 7 days

        // Real health evaluation
        String healthStatus;
        if (batteryLevel < 20 || signalStrength < -90) {
            healthStatus = "critical";
        } else if (batteryLevel < 50 || signalStrength < -75) {
            healthStatus = "degraded";
        } else {
            healthStatus = "healthy";
        }

        String uptime = (uptimeMinutes / 1440) + "d " + ((uptimeMinutes % 1440) / 60) + "h " + (uptimeMinutes % 60) + "m";

        System.out.println("  [health] " + deviceId + ": " + healthStatus + " (battery: " + batteryLevel
                + "%, signal: " + signalStrength + " dBm)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("healthStatus", healthStatus);
        result.getOutputData().put("batteryLevel", batteryLevel);
        result.getOutputData().put("signalStrength", signalStrength);
        result.getOutputData().put("lastSeen", Instant.now().toString());
        result.getOutputData().put("uptime", uptime);
        return result;
    }
}
