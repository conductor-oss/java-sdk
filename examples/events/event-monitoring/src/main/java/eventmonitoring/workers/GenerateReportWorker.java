package eventmonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Generates a monitoring report from throughput, latency, and error analysis.
 * Input: pipeline, throughput, latency, errorRate
 * Output: reportGenerated (true), alertLevel ("normal" or "warning" or "critical")
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_generate_report";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String pipeline = (String) task.getInputData().get("pipeline");
        if (pipeline == null) {
            pipeline = "unknown-pipeline";
        }

        Map<String, Object> errorRate = (Map<String, Object>) task.getInputData().get("errorRate");

        String alertLevel = "normal";
        if (errorRate != null && errorRate.get("percentage") != null) {
            double pct = Double.parseDouble(String.valueOf(errorRate.get("percentage")));
            if (pct >= 10.0) {
                alertLevel = "critical";
            } else if (pct >= 5.0) {
                alertLevel = "warning";
            }
        }

        System.out.println("  [em_generate_report] Report for pipeline '" + pipeline + "' — alert level: " + alertLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportGenerated", true);
        result.getOutputData().put("alertLevel", alertLevel);
        return result;
    }
}
