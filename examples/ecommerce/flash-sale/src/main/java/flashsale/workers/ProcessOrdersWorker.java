package flashsale.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class ProcessOrdersWorker implements Worker {
    @Override public String getTaskDefName() { return "fls_process_orders"; }
    @Override public TaskResult execute(Task task) {
        int available = 300; Object a = task.getInputData().get("availableStock"); if (a instanceof Number) available = ((Number)a).intValue();
        int ordersFilled = Math.min(available, 275);
        double revenue = ordersFilled * 62.50;
        System.out.printf("  [orders] Processed %d orders, revenue: $%.2f%n", ordersFilled, revenue);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("ordersFilled", ordersFilled); o.put("revenue", String.format("%.2f", revenue)); o.put("remainingStock", available - ordersFilled);
        r.setOutputData(o); return r;
    }
}
