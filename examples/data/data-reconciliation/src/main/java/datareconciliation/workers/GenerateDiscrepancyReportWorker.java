package datareconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates a discrepancy report from comparison results.
 * Input: matched, mismatched, missingInA, missingInB
 * Output: reconciliationRate, discrepancies
 */
public class GenerateDiscrepancyReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rc_generate_discrepancy_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> matched = (List<String>) task.getInputData().getOrDefault("matched", List.of());
        List<Map<String, Object>> mismatched = (List<Map<String, Object>>) task.getInputData().getOrDefault("mismatched", List.of());
        List<String> missingA = (List<String>) task.getInputData().getOrDefault("missingInA", List.of());
        List<String> missingB = (List<String>) task.getInputData().getOrDefault("missingInB", List.of());

        int total = matched.size() + mismatched.size() + missingA.size() + missingB.size();
        String rate = total == 0 ? "0.0%" : String.format("%.1f%%", (matched.size() * 100.0) / total);

        List<String> discrepancies = new ArrayList<>();
        for (Map<String, Object> m : mismatched) {
            List<String> fields = (List<String>) m.get("differingFields");
            discrepancies.add(m.get("key") + ": fields differ (" + String.join(", ", fields) + ")");
        }
        for (String k : missingB) {
            discrepancies.add(k + ": missing in source B");
        }
        for (String k : missingA) {
            discrepancies.add(k + ": missing in source A");
        }

        System.out.println("  [report] Reconciliation rate: " + rate + ", " + discrepancies.size() + " discrepancies found");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reconciliationRate", rate);
        result.getOutputData().put("discrepancies", discrepancies);
        return result;
    }
}
