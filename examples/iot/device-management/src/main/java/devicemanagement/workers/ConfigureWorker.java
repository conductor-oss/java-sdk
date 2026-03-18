package devicemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures device settings. Real configuration based on device type.
 */
public class ConfigureWorker implements Worker {
    @Override public String getTaskDefName() { return "dev_configure"; }

    @Override public TaskResult execute(Task task) {
        String deviceType = (String) task.getInputData().get("deviceType");
        if (deviceType == null) deviceType = "unknown";

        // Real configuration based on device type
        int reportingInterval;
        List<String> telemetryTopics = new ArrayList<>();

        switch (deviceType.toLowerCase()) {
            case "sensor" -> {
                reportingInterval = 30;
                telemetryTopics.addAll(List.of("temperature", "humidity", "battery"));
            }
            case "camera" -> {
                reportingInterval = 5;
                telemetryTopics.addAll(List.of("motion_detected", "stream_status", "storage_usage"));
            }
            case "thermostat" -> {
                reportingInterval = 60;
                telemetryTopics.addAll(List.of("temperature", "setpoint", "hvac_status", "energy_usage"));
            }
            case "gateway" -> {
                reportingInterval = 10;
                telemetryTopics.addAll(List.of("connected_devices", "throughput", "error_rate"));
            }
            default -> {
                reportingInterval = 60;
                telemetryTopics.add("status");
            }
        }

        System.out.println("  [configure] " + deviceType + ": interval=" + reportingInterval + "s, topics=" + telemetryTopics);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("configured", true);
        result.getOutputData().put("reportingInterval", reportingInterval);
        result.getOutputData().put("telemetryTopics", telemetryTopics);
        result.getOutputData().put("shadowCreated", true);
        return result;
    }
}
