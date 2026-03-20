package workflowvariables.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Calculates shipping cost based on the discounted subtotal, item count, and customer tier.
 * Gold tier or orders over $100 get free shipping.
 * Otherwise shipping is $5.99 + $1.50 per additional item.
 */
public class CalcShippingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wv_calc_shipping";
    }

    @Override
    public TaskResult execute(Task task) {
        double afterDiscount = ((Number) task.getInputData().get("afterDiscount")).doubleValue();
        int itemCount = ((Number) task.getInputData().get("itemCount")).intValue();
        String tier = (String) task.getInputData().get("tier");

        // Free shipping for gold tier or orders over $100
        boolean freeShipping = "gold".equals(tier) || afterDiscount > 100;
        double shippingCost = freeShipping ? 0.0 : 5.99 + (itemCount - 1) * 1.5;
        double finalTotal = Math.round((afterDiscount + shippingCost) * 100.0) / 100.0;

        System.out.println("  [shipping] " + (freeShipping ? "FREE" : "$" + shippingCost)
                + " -> Total: $" + finalTotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("shippingCost", shippingCost);
        result.getOutputData().put("freeShipping", freeShipping);
        result.getOutputData().put("finalTotal", finalTotal);
        return result;
    }
}
