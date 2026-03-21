package riskassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes operational risk from incident, control gap, and maturity data.
 */
public class OperationalRiskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rsk_operational_risk";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("operationalData");
        if (data == null) data = Map.of();

        int incidents = toInt(data.get("incidents"));
        int controlGaps = toInt(data.get("controlGaps"));
        int processMaturity = toInt(data.get("processMaturity"));
        int riskScore = (int) Math.round(incidents * 10 + controlGaps * 5 + (100 - processMaturity) / 5.0);

        System.out.println("  [ops] Incidents: " + incidents + ", Control gaps: " + controlGaps + ", Risk: " + riskScore);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskScore", riskScore);
        result.getOutputData().put("capitalCharge", 750000);
        result.getOutputData().put("methodology", "Basic Indicator Approach");
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }
}
