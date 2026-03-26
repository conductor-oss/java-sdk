package pubsubconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes decoded sensor data by checking thresholds and generating alerts.
 * Thresholds: temperature high=85, temperature low=55, humidity high=70.
 * With default values (temp=72.4, humidity=45.2) no alerts are produced.
 */
public class PsProcessDataWorker implements Worker {

    private static final double TEMP_HIGH = 85.0;
    private static final double TEMP_LOW = 55.0;
    private static final double HUMIDITY_HIGH = 70.0;

    @Override
    public String getTaskDefName() {
        return "ps_process_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> decodedData = (Map<String, Object>) task.getInputData().get("decodedData");
        if (decodedData == null) {
            decodedData = Map.of();
        }

        String eventType = (String) task.getInputData().get("eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = "iot.sensor.reading";
        }

        System.out.println("  [ps_process_data] Processing event type: " + eventType);

        String sensorId = (String) decodedData.getOrDefault("sensorId", "unknown");

        double temperature = toDouble(decodedData.get("temperature"), 0.0);
        double humidity = toDouble(decodedData.get("humidity"), 0.0);

        // Check thresholds and generate alerts
        List<String> alerts = new ArrayList<>();
        if (temperature > TEMP_HIGH) {
            alerts.add("HIGH_TEMPERATURE: " + temperature + " exceeds " + TEMP_HIGH);
        }
        if (temperature < TEMP_LOW) {
            alerts.add("LOW_TEMPERATURE: " + temperature + " below " + TEMP_LOW);
        }
        if (humidity > HUMIDITY_HIGH) {
            alerts.add("HIGH_HUMIDITY: " + humidity + " exceeds " + HUMIDITY_HIGH);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("result", "stored");
        result.getOutputData().put("sensorId", sensorId);
        result.getOutputData().put("alertCount", alerts.size());
        result.getOutputData().put("alerts", alerts);
        return result;
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
