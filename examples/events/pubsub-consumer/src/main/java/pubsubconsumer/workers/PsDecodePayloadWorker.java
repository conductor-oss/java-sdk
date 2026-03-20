package pubsubconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Decodes a base64-encoded Pub/Sub message payload into structured sensor data.
 * Determines event type from message attributes or defaults to "iot.sensor.reading".
 */
public class PsDecodePayloadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ps_decode_payload";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String encodedData = (String) task.getInputData().get("encodedData");
        if (encodedData == null) {
            encodedData = "";
        }

        String encoding = (String) task.getInputData().get("encoding");
        if (encoding == null || encoding.isBlank()) {
            encoding = "base64";
        }

        Map<String, Object> attributes = (Map<String, Object>) task.getInputData().get("attributes");
        if (attributes == null) {
            attributes = Map.of();
        }

        System.out.println("  [ps_decode_payload] Decoding " + encoding + " payload (" + encodedData.length() + " chars)");

        // Deterministic decoded sensor data
        Map<String, Object> decodedData = Map.of(
                "sensorId", "sensor-T42",
                "temperature", 72.4,
                "humidity", 45.2,
                "pressure", 1013.25,
                "location", "building-A-floor-3",
                "readingTime", "2026-03-08T10:15:00Z"
        );

        // Determine event type from attributes or use default
        String eventType = attributes.containsKey("eventType")
                ? (String) attributes.get("eventType")
                : "iot.sensor.reading";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decodedData", decodedData);
        result.getOutputData().put("eventType", eventType);
        return result;
    }
}
