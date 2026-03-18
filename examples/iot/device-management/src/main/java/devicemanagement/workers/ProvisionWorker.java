package devicemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Provisions credentials for a device. Real certificate ID generation.
 */
public class ProvisionWorker implements Worker {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override public String getTaskDefName() { return "dev_provision"; }

    @Override public TaskResult execute(Task task) {
        String deviceId = (String) task.getInputData().get("deviceId");
        if (deviceId == null) deviceId = "unknown";

        // Generate real certificate ID
        byte[] certBytes = new byte[8];
        SECURE_RANDOM.nextBytes(certBytes);
        String certificateId = "CERT-" + HexFormat.of().formatHex(certBytes).substring(0, 12).toUpperCase();

        String mqttEndpoint = "mqtts://iot.example.com:8883";
        String thingName = deviceId;

        System.out.println("  [provision] " + deviceId + " -> cert " + certificateId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("certificateId", certificateId);
        result.getOutputData().put("mqttEndpoint", mqttEndpoint);
        result.getOutputData().put("thingName", thingName);
        result.getOutputData().put("provisionedAt", Instant.now().toString());
        return result;
    }
}
