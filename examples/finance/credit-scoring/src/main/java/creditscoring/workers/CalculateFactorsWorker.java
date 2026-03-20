package creditscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Calculates weighted credit score factors from credit history.
 * Input: applicantId, creditHistory
 * Output: factors (paymentHistory, utilization, creditAge, creditMix, newCredit)
 */
public class CalculateFactorsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "csc_calculate_factors";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> history = (Map<String, Object>) task.getInputData().get("creditHistory");
        if (history == null) history = Map.of();

        int totalAccounts = toInt(history.get("totalAccounts"));
        int utilization = toInt(history.get("utilization"));

        System.out.println("  [factors] Calculating score factors -- " + totalAccounts + " accounts, " + utilization + "% utilization");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("factors", Map.of(
                "paymentHistory", Map.of("weight", 35, "score", 92, "detail", "1 late payment in 12 years"),
                "utilization", Map.of("weight", 30, "score", 85, "detail", "28% credit utilization"),
                "creditAge", Map.of("weight", 15, "score", 95, "detail", "12 years average account age"),
                "creditMix", Map.of("weight", 10, "score", 88, "detail", "Good mix of credit types"),
                "newCredit", Map.of("weight", 10, "score", 80, "detail", "2 inquiries in last 12 months")));
        return result;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
