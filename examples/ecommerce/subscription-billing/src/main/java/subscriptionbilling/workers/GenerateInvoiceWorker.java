package subscriptionbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class GenerateInvoiceWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();
    private static final Map<String, Double> PLAN_PRICES = Map.of(
            "starter", 9.99, "professional", 29.99, "enterprise", 99.99);

    @Override
    public String getTaskDefName() { return "sub_generate_invoice"; }

    @Override
    public TaskResult execute(Task task) {
        String invoiceId = "INV-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + "-" + COUNTER.incrementAndGet();
        String plan = (String) task.getInputData().getOrDefault("plan", "professional");
        double amount = PLAN_PRICES.getOrDefault(plan, 29.99);

        System.out.println("  [invoice] " + invoiceId + ": " + plan + " plan, $" + amount
                + " for " + task.getInputData().get("periodStart") + " - " + task.getInputData().get("periodEnd"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("invoiceId", invoiceId);
        output.put("amount", amount);
        output.put("lineItems", List.of(Map.of("description", plan + " plan", "amount", amount)));
        result.setOutputData(output);
        return result;
    }
}
