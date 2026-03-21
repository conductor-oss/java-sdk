package workflowvariables.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Builds a final order summary by pulling data from workflow input and
 * outputs of all preceding tasks. Demonstrates how workflow variables
 * persist and flow across task boundaries.
 */
public class BuildSummaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wv_build_summary";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        String customerTier = (String) task.getInputData().get("customerTier");
        double subtotal = ((Number) task.getInputData().get("subtotal")).doubleValue();
        double discount = ((Number) task.getInputData().get("discount")).doubleValue();
        double discountRate = ((Number) task.getInputData().get("discountRate")).doubleValue();
        double shipping = ((Number) task.getInputData().get("shipping")).doubleValue();
        double finalTotal = ((Number) task.getInputData().get("finalTotal")).doubleValue();

        int discountPct = (int) Math.round(discountRate * 100);
        String summary = String.join("\n",
                "Order " + orderId + " (" + customerTier + " tier)",
                "  Subtotal: $" + subtotal,
                "  Discount: -$" + discount + " (" + discountPct + "%)",
                "  Shipping: $" + shipping,
                "  Total: $" + finalTotal
        );

        System.out.println("  [summary]\n" + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
