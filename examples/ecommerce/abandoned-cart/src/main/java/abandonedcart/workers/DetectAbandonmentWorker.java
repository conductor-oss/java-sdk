package abandonedcart.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class DetectAbandonmentWorker implements Worker {
    @Override public String getTaskDefName() { return "abc_detect_abandonment"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [detect] Cart " + task.getInputData().get("cartId") + " abandoned by " + task.getInputData().get("customerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("items", List.of(Map.of("sku", "SKU-101", "name", "Wireless Mouse", "price", 29.99), Map.of("sku", "SKU-205", "name", "USB Hub", "price", 19.99)));
        o.put("abandonedAt", Instant.now().toString()); o.put("hoursInactive", 4);
        r.setOutputData(o); return r;
    }
}
