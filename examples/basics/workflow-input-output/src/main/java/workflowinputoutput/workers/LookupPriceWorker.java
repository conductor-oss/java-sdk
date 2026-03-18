package workflowinputoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Looks up product price from a built-in catalog and calculates subtotal.
 *
 * Input:  productId, quantity
 * Output: productName, unitPrice, quantity, subtotal
 */
public class LookupPriceWorker implements Worker {

    private static final Map<String, ProductInfo> CATALOG = Map.of(
            "PROD-001", new ProductInfo("Wireless Keyboard", 79.99),
            "PROD-002", new ProductInfo("27\" Monitor", 349.99),
            "PROD-003", new ProductInfo("USB-C Hub", 49.99)
    );

    private static final ProductInfo DEFAULT_PRODUCT = new ProductInfo("Unknown Product", 0.0);

    @Override
    public String getTaskDefName() {
        return "lookup_price";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        int quantity = toInt(task.getInputData().get("quantity"), 1);

        ProductInfo product = CATALOG.getOrDefault(productId, DEFAULT_PRODUCT);
        double subtotal = product.price() * quantity;

        System.out.printf("  [lookup_price] %s x%d = $%.2f%n", product.name(), quantity, subtotal);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("productName", product.name());
        result.getOutputData().put("unitPrice", product.price());
        result.getOutputData().put("quantity", quantity);
        result.getOutputData().put("subtotal", subtotal);
        return result;
    }

    private static int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private record ProductInfo(String name, double price) {}
}
