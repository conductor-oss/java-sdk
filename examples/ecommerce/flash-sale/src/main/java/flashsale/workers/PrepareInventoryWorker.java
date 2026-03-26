package flashsale.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class PrepareInventoryWorker implements Worker {
    @Override public String getTaskDefName() { return "fls_prepare_inventory"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [prepare] Reserving inventory for \"" + task.getInputData().get("saleName") + "\"");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("inventory", List.of(Map.of("sku", "FL-001", "name", "Designer Sneakers", "stock", 200, "salePrice", 49.99), Map.of("sku", "FL-002", "name", "Leather Jacket", "stock", 100, "salePrice", 89.99)));
        o.put("totalStock", 300); r.setOutputData(o); return r;
    }
}
