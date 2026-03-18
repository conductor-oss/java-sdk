package workflowinputoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Calculates sales tax on the discounted total.
 *
 * Input:  discountedTotal
 * Output: taxRate, taxAmount, finalTotal
 */
public class CalculateTaxWorker implements Worker {

    private static final double TAX_RATE = 0.0825;

    @Override
    public String getTaskDefName() {
        return "calculate_tax";
    }

    @Override
    public TaskResult execute(Task task) {
        double discountedTotal = toDouble(task.getInputData().get("discountedTotal"));

        double taxAmount = discountedTotal * TAX_RATE;
        double finalTotal = discountedTotal + taxAmount;

        System.out.printf("  [calculate_tax] $%.2f + %.2f%% tax ($%.2f) = $%.2f%n",
                discountedTotal, TAX_RATE * 100, taxAmount, finalTotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("taxRate", TAX_RATE);
        result.getOutputData().put("taxAmount", taxAmount);
        result.getOutputData().put("finalTotal", finalTotal);
        return result;
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
