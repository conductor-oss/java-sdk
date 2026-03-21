package eventcorrelation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes correlated event data and determines the action to take.
 * Returns deterministic output with fixed timestamps.
 */
public class ProcessCorrelatedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_process_correlated";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String correlationId = (String) task.getInputData().get("correlationId");
        Map<String, Object> correlatedData = (Map<String, Object>) task.getInputData().get("correlatedData");
        Object matchScoreObj = task.getInputData().get("matchScore");
        double matchScore = matchScoreObj instanceof Number ? ((Number) matchScoreObj).doubleValue() : 0.0;

        String orderId = correlatedData != null ? (String) correlatedData.get("orderId") : "UNKNOWN";
        String paymentMethod = correlatedData != null ? (String) correlatedData.get("paymentMethod") : "unknown";
        String carrier = correlatedData != null ? (String) correlatedData.get("carrier") : "unknown";

        String summary = "Order " + orderId + " fully correlated: paid via " + paymentMethod
                + ", shipping via " + carrier;

        System.out.println("  [ec_process_correlated] Processing correlated data for: " + correlationId);
        System.out.println("    -> " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "fulfill_order");
        result.getOutputData().put("processedAt", "2026-01-15T10:03:00Z");
        result.getOutputData().put("summary", summary);
        return result;
    }
}
