package inventoryoptimization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class AnalyzeStockWorker implements Worker {
    @Override public String getTaskDefName() { return "io_analyze_stock"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<String> skus = (List<String>) task.getInputData().get("skuList");
        if (skus == null) skus = List.of();
        Random rand = new Random(42);
        List<Map<String, Object>> stockLevels = skus.stream()
            .map(sku -> Map.<String, Object>of("sku", sku, "current", rand.nextInt(500) + 50, "dailyUsage", rand.nextInt(30) + 5))
            .collect(Collectors.toList());
        System.out.println("  [analyze] Analyzed " + stockLevels.size() + " SKUs in " + task.getInputData().get("warehouse"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("stockLevels", stockLevels); r.getOutputData().put("skuCount", stockLevels.size());
        return r;
    }
}
