package exclusivejoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects the best vendor from the three parallel vendor responses.
 * Picks the vendor with the lowest price. If prices are equal, picks
 * the vendor with the fastest response time.
 */
public class SelectBestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ej_select_best";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> vendorA = (Map<String, Object>) task.getInputData().get("vendorA");
        Map<String, Object> vendorB = (Map<String, Object>) task.getInputData().get("vendorB");
        Map<String, Object> vendorC = (Map<String, Object>) task.getInputData().get("vendorC");

        List<Map<String, Object>> vendors = List.of(vendorA, vendorB, vendorC);

        System.out.println("  [ej_select_best] Comparing vendor responses:");

        Map<String, Object> best = null;
        double bestPrice = Double.MAX_VALUE;
        int bestResponseTime = Integer.MAX_VALUE;

        for (Map<String, Object> vendor : vendors) {
            String name = (String) vendor.get("vendor");
            double price = ((Number) vendor.get("price")).doubleValue();
            int responseTime = ((Number) vendor.get("responseTime")).intValue();

            System.out.println("    -> Vendor " + name + ": $" + price + " (" + responseTime + "ms)");

            if (price < bestPrice || (price == bestPrice && responseTime < bestResponseTime)) {
                best = vendor;
                bestPrice = price;
                bestResponseTime = responseTime;
            }
        }

        String winner = (String) best.get("vendor");
        System.out.println("  [ej_select_best] Winner: Vendor " + winner +
                " ($" + bestPrice + ", " + bestResponseTime + "ms)");

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("winner", winner);
        output.put("price", bestPrice);
        output.put("responseTime", bestResponseTime);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bestVendor", output);
        return result;
    }
}
