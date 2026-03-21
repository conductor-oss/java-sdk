package streamprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DetectAnomaliesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_detect_anomalies";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggregates = (List<Map<String, Object>>) task.getInputData().get("aggregates");
        if (aggregates == null) aggregates = List.of();

        double totalAvg = 0;
        for (Map<String, Object> a : aggregates) {
            Object avgObj = a.get("avg");
            if (avgObj instanceof Number) totalAvg += ((Number) avgObj).doubleValue();
        }
        double globalAvg = aggregates.isEmpty() ? 0 : totalAvg / aggregates.size();

        List<Map<String, Object>> anomalies = new ArrayList<>();
        for (Map<String, Object> a : aggregates) {
            Object avgObj = a.get("avg");
            double avg = avgObj instanceof Number ? ((Number) avgObj).doubleValue() : 0;
            double deviation = Math.abs(avg - globalAvg);
            if (deviation > globalAvg * 0.5) {
                Map<String, Object> anomaly = new LinkedHashMap<>();
                anomaly.put("windowStart", a.get("windowStart"));
                anomaly.put("avg", avg);
                anomaly.put("deviation", Math.round(deviation * 100.0) / 100.0);
                anomalies.add(anomaly);
            }
        }

        System.out.println("  [anomaly] Detected " + anomalies.size() + " anomalous windows");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("anomalies", anomalies);
        result.getOutputData().put("anomalyCount", anomalies.size());
        result.getOutputData().put("globalAvg", globalAvg);
        return result;
    }
}
