package energymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Monitors energy consumption and returns readings with total kWh.
 * Input: buildingId, period
 * Output: consumption (list of readings), totalKwh
 */
public class MonitorConsumptionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "erg_monitor_consumption";
    }

    @Override
    public TaskResult execute(Task task) {
        String buildingId = (String) task.getInputData().getOrDefault("buildingId", "unknown");

        List<Map<String, Object>> readings = List.of(
                Map.of("hour", "00:00", "kw", 12.3),
                Map.of("hour", "06:00", "kw", 18.7),
                Map.of("hour", "12:00", "kw", 45.2),
                Map.of("hour", "18:00", "kw", 38.1)
        );

        double totalKwh = readings.stream()
                .mapToDouble(r -> ((Number) r.get("kw")).doubleValue() * 6)
                .sum();

        System.out.println("  [monitor] Building " + buildingId + ": " + String.format("%.1f", totalKwh) + " kWh");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("consumption", readings);
        result.getOutputData().put("totalKwh", String.format("%.1f", totalKwh));
        return result;
    }
}
