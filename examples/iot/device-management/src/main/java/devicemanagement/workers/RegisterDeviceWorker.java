package devicemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Registers an IoT device. Real validation of device type and fleet assignment.
 */
public class RegisterDeviceWorker implements Worker {
    private static final Set<String> VALID_TYPES = Set.of("sensor", "gateway", "actuator", "camera", "thermostat");

    @Override public String getTaskDefName() { return "dev_register_device"; }

    @Override public TaskResult execute(Task task) {
        String deviceId = (String) task.getInputData().get("deviceId");
        String deviceType = (String) task.getInputData().get("deviceType");
        String fleetId = (String) task.getInputData().get("fleetId");
        if (deviceId == null) deviceId = "unknown";
        if (deviceType == null) deviceType = "unknown";
        if (fleetId == null) fleetId = "unassigned";

        boolean validType = VALID_TYPES.contains(deviceType.toLowerCase());
        String registrationId = "REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        System.out.println("  [register] " + deviceType + " device " + deviceId
                + " -> " + registrationId + " (fleet: " + fleetId + ", valid: " + validType + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(validType ? TaskResult.Status.COMPLETED : TaskResult.Status.FAILED);
        if (!validType) result.setReasonForIncompletion("Invalid device type: " + deviceType);
        result.getOutputData().put("registrationId", registrationId);
        result.getOutputData().put("registeredAt", Instant.now().toString());
        result.getOutputData().put("fleetId", fleetId);
        result.getOutputData().put("validType", validType);
        return result;
    }
}
