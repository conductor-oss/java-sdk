package pubsubconsumer.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PsProcessDataWorkerTest {

    private final PsProcessDataWorker worker = new PsProcessDataWorker();

    @Test
    void taskDefName() {
        assertEquals("ps_process_data", worker.getTaskDefName());
    }

    @Test
    void processesNormalReadingWithNoAlerts() {
        Task task = taskWith(Map.of(
                "decodedData", Map.of(
                        "sensorId", "sensor-T42",
                        "temperature", 72.4,
                        "humidity", 45.2,
                        "pressure", 1013.25),
                "eventType", "iot.sensor.reading"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("stored", result.getOutputData().get("result"));
        assertEquals("sensor-T42", result.getOutputData().get("sensorId"));
        assertEquals(0, result.getOutputData().get("alertCount"));
    }

    @Test
    void returnsEmptyAlertsForNormalReading() {
        Task task = taskWith(Map.of(
                "decodedData", Map.of(
                        "sensorId", "sensor-T42",
                        "temperature", 72.4,
                        "humidity", 45.2),
                "eventType", "iot.sensor.reading"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> alerts = (List<String>) result.getOutputData().get("alerts");
        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void alertsOnHighTemperature() {
        Task task = taskWith(Map.of(
                "decodedData", Map.of(
                        "sensorId", "sensor-H01",
                        "temperature", 90.0,
                        "humidity", 45.2),
                "eventType", "iot.sensor.reading"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("alertCount"));

        @SuppressWarnings("unchecked")
        List<String> alerts = (List<String>) result.getOutputData().get("alerts");
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).contains("HIGH_TEMPERATURE"));
    }

    @Test
    void alertsOnLowTemperature() {
        Task task = taskWith(Map.of(
                "decodedData", Map.of(
                        "sensorId", "sensor-C01",
                        "temperature", 50.0,
                        "humidity", 40.0),
                "eventType", "iot.sensor.reading"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("alertCount"));

        @SuppressWarnings("unchecked")
        List<String> alerts = (List<String>) result.getOutputData().get("alerts");
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).contains("LOW_TEMPERATURE"));
    }

    @Test
    void alertsOnHighHumidity() {
        Task task = taskWith(Map.of(
                "decodedData", Map.of(
                        "sensorId", "sensor-W01",
                        "temperature", 72.0,
                        "humidity", 75.0),
                "eventType", "iot.sensor.reading"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("alertCount"));

        @SuppressWarnings("unchecked")
        List<String> alerts = (List<String>) result.getOutputData().get("alerts");
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).contains("HIGH_HUMIDITY"));
    }

    @Test
    void multipleAlerts() {
        Task task = taskWith(Map.of(
                "decodedData", Map.of(
                        "sensorId", "sensor-X01",
                        "temperature", 90.0,
                        "humidity", 80.0),
                "eventType", "iot.sensor.reading"));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("alertCount"));

        @SuppressWarnings("unchecked")
        List<String> alerts = (List<String>) result.getOutputData().get("alerts");
        assertEquals(2, alerts.size());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("stored", result.getOutputData().get("result"));
        assertEquals("unknown", result.getOutputData().get("sensorId"));
    }

    @Test
    void handlesNullDecodedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("decodedData", null);
        input.put("eventType", "iot.sensor.reading");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
        assertEquals("unknown", result.getOutputData().get("sensorId"));
    }

    @Test
    void defaultsEventTypeWhenMissing() {
        Task task = taskWith(Map.of(
                "decodedData", Map.of("sensorId", "sensor-T42", "temperature", 72.4, "humidity", 45.2)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
