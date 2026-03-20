package pubsubconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PsDecodePayloadWorkerTest {

    private final PsDecodePayloadWorker worker = new PsDecodePayloadWorker();

    @Test
    void taskDefName() {
        assertEquals("ps_decode_payload", worker.getTaskDefName());
    }

    @Test
    void decodesStandardPayload() {
        Task task = taskWith(Map.of(
                "encodedData", "eyJzZW5zb3JJZCI6...",
                "encoding", "base64",
                "attributes", Map.of("eventType", "iot.sensor.reading")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("decodedData"));
        assertEquals("iot.sensor.reading", result.getOutputData().get("eventType"));
    }

    @Test
    void returnsDecodedSensorData() {
        Task task = taskWith(Map.of(
                "encodedData", "eyJzZW5zb3JJZCI6...",
                "encoding", "base64",
                "attributes", Map.of("eventType", "iot.sensor.reading")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> decodedData = (Map<String, Object>) result.getOutputData().get("decodedData");
        assertNotNull(decodedData);
        assertEquals("sensor-T42", decodedData.get("sensorId"));
        assertEquals(72.4, decodedData.get("temperature"));
        assertEquals(45.2, decodedData.get("humidity"));
        assertEquals(1013.25, decodedData.get("pressure"));
        assertEquals("building-A-floor-3", decodedData.get("location"));
        assertEquals("2026-03-08T10:15:00Z", decodedData.get("readingTime"));
    }

    @Test
    void usesEventTypeFromAttributes() {
        Task task = taskWith(Map.of(
                "encodedData", "data",
                "encoding", "base64",
                "attributes", Map.of("eventType", "iot.sensor.reading")));
        TaskResult result = worker.execute(task);

        assertEquals("iot.sensor.reading", result.getOutputData().get("eventType"));
    }

    @Test
    void defaultsEventTypeWhenMissing() {
        Task task = taskWith(Map.of(
                "encodedData", "data",
                "encoding", "base64",
                "attributes", Map.of("sensorType", "environmental")));
        TaskResult result = worker.execute(task);

        assertEquals("iot.sensor.reading", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesEmptyAttributes() {
        Task task = taskWith(Map.of(
                "encodedData", "data",
                "encoding", "base64",
                "attributes", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("iot.sensor.reading", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("decodedData"));
        assertEquals("iot.sensor.reading", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullEncodedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("encodedData", null);
        input.put("encoding", "base64");
        input.put("attributes", Map.of("eventType", "custom.event"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("custom.event", result.getOutputData().get("eventType"));
    }

    @Test
    void handlesNullAttributes() {
        Map<String, Object> input = new HashMap<>();
        input.put("encodedData", "payload");
        input.put("encoding", "base64");
        input.put("attributes", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("iot.sensor.reading", result.getOutputData().get("eventType"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
