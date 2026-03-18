package claimsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;

/**
 * Settles claim amount. Real computation:
 * - Subtracts deductible based on damage category
 * - Determines payment method based on amount
 * - Computes estimated payment date
 */
public class SettleAmountWorker implements Worker {

    @Override
    public String getTaskDefName() { return "clp_settle_amount"; }

    @Override
    public TaskResult execute(Task task) {
        Object rawAssessed = task.getInputData().get("assessedAmount");
        String damageCategory = (String) task.getInputData().get("damageCategory");
        if (damageCategory == null) damageCategory = "moderate";

        double assessed = 0;
        if (rawAssessed instanceof Number) {
            assessed = ((Number) rawAssessed).doubleValue();
        } else if (rawAssessed instanceof String) {
            try { assessed = Double.parseDouble((String) rawAssessed); } catch (NumberFormatException ignored) {}
        }

        // Real deductible based on damage category
        long deductible = switch (damageCategory) {
            case "minor" -> 250;
            case "moderate" -> 500;
            case "major" -> 1000;
            case "severe" -> 2000;
            default -> 500;
        };

        long settled = Math.max(0, (long) assessed - deductible);

        // Real payment method determination based on amount
        String paymentMethod;
        if (settled > 10000) {
            paymentMethod = "wire_transfer";
        } else if (settled > 1000) {
            paymentMethod = "direct_deposit";
        } else {
            paymentMethod = "check";
        }

        // Payment date: 7 business days for direct deposit, 14 for check, 3 for wire
        int daysToPayment = switch (paymentMethod) {
            case "wire_transfer" -> 3;
            case "direct_deposit" -> 7;
            default -> 14;
        };
        String estimatedPaymentDate = LocalDate.now().plusDays(daysToPayment).toString();

        System.out.println("  [settle] Assessed: $" + (long) assessed + ", Deductible: $" + deductible
                + ", Settled: $" + settled + " via " + paymentMethod);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("settledAmount", settled);
        result.getOutputData().put("deductible", deductible);
        result.getOutputData().put("paymentMethod", paymentMethod);
        result.getOutputData().put("estimatedPaymentDate", estimatedPaymentDate);
        return result;
    }
}
