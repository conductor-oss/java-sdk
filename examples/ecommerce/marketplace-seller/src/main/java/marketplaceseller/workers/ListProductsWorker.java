package marketplaceseller.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class ListProductsWorker implements Worker {
    @Override public String getTaskDefName() { return "mkt_list_products"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [list] Listing products for store " + task.getInputData().get("storeId") + " in " + task.getInputData().get("category"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("productCount", 12);
        o.put("products", List.of(Map.of("sku", "S-001", "name", "Artisan Candle", "price", 24.99), Map.of("sku", "S-002", "name", "Hand Soap Set", "price", 18.50)));
        r.setOutputData(o); return r;
    }
}
