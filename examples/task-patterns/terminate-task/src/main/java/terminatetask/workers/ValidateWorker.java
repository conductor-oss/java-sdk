package terminatetask.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates an order by checking amount and currency constraints.
 *
 * Validation rules:
 * - Amount must be positive (greater than 0)
 * - Amount must not exceed $10,000
 * - Currency must be one of: USD, EUR, GBP
 *
 * Output:
 * - valid (boolean): whether all validations passed
 * - reason (String|null): semicolon-joined error messages, or null if valid
 * - errors (List): individual error messages
 */
public class ValidateWorker implements Worker {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP");
    private static final double MAX_AMOUNT = 10_000;

    @Override
    public String getTaskDefName() {
        return "term_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Number amountNum = (Number) task.getInputData().get("amount");
        String currency = (String) task.getInputData().get("currency");

        double amount = amountNum != null ? amountNum.doubleValue() : 0;

        List<String> errors = new ArrayList<>();

        if (amount <= 0) {
            errors.add("Amount must be positive");
        }
        if (amount > MAX_AMOUNT) {
            errors.add("Amount exceeds limit ($10,000)");
        }
        if (currency == null || !SUPPORTED_CURRENCIES.contains(currency)) {
            errors.add("Unsupported currency: " + currency);
        }

        boolean valid = errors.isEmpty();

        System.out.println("  [validate] Order " + orderId + ": $" + amount + " " + currency
                + " -> " + (valid ? "VALID" : "INVALID"));
        if (!valid) {
            System.out.println("    Errors: " + String.join(", ", errors));
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", valid);
        result.getOutputData().put("reason", valid ? null : String.join("; ", errors));
        result.getOutputData().put("errors", errors);
        return result;
    }
}
