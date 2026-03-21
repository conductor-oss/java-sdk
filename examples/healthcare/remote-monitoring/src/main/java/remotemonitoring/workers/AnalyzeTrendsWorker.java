package remotemonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class AnalyzeTrendsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "rpm_analyze_trends"; }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> vitals = (Map<String, Object>) task.getInputData().getOrDefault("vitals", Map.of());
        List<String> alerts = new ArrayList<>();
        Map<String, Object> bp = (Map<String, Object>) vitals.getOrDefault("bloodPressure", Map.of());
        int systolic = bp.get("systolic") instanceof Number ? ((Number) bp.get("systolic")).intValue() : 0;
        if (systolic > 140) alerts.add("High blood pressure");
        int glucose = vitals.get("glucose") instanceof Number ? ((Number) vitals.get("glucose")).intValue() : 0;
        if (glucose > 180) alerts.add("Elevated glucose");
        int spo2 = vitals.get("oxygenSaturation") instanceof Number ? ((Number) vitals.get("oxygenSaturation")).intValue() : 100;
        if (spo2 < 95) alerts.add("Low oxygen saturation");
        String status = alerts.isEmpty() ? "normal" : "alert";
        System.out.println("  [analyze] Status: " + status + ", Alerts: " + alerts.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", status);
        output.put("alerts", alerts);
        output.put("trendDirection", "worsening");
        result.setOutputData(output);
        return result;
    }
}
