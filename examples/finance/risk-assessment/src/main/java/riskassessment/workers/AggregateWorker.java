package riskassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Aggregates market, credit, and operational risk scores into an overall assessment.
 */
public class AggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rsk_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        int market = toInt(task.getInputData().get("marketRisk"));
        int credit = toInt(task.getInputData().get("creditRisk"));
        int ops = toInt(task.getInputData().get("operationalRisk"));

        int overall = (int) Math.round(market * 0.4 + credit * 0.35 + ops * 0.25);
        String category = overall >= 60 ? "High" : overall >= 30 ? "Moderate" : "Low";

        System.out.println("  [aggregate] Market: " + market + ", Credit: " + credit +
                ", Ops: " + ops + " -> Overall: " + overall + " (" + category + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallRisk", overall);
        result.getOutputData().put("riskCategory", category);
        result.getOutputData().put("var95", 2450000);
        result.getOutputData().put("capitalAdequate", !"High".equals(category));
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
