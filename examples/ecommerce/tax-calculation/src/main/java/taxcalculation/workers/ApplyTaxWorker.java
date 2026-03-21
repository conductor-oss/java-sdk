package taxcalculation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class ApplyTaxWorker implements Worker {
    @Override public String getTaskDefName() { return "tax_apply"; }
    @Override public TaskResult execute(Task task) {
        double subtotal = toDouble(task.getInputData().get("subtotal"));
        double rate = toDouble(task.getInputData().get("taxRate"));
        double taxAmount = Math.round(subtotal * rate * 100.0) / 100.0;
        double total = Math.round((subtotal + taxAmount) * 100.0) / 100.0;
        System.out.printf("  [apply] Subtotal: $%.2f, Tax: $%.2f, Total: $%.2f%n", subtotal, taxAmount, total);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("taxAmount", taxAmount); o.put("total", total); o.put("taxRate", rate);
        r.setOutputData(o); return r;
    }
    private double toDouble(Object o) { if (o instanceof Number) return ((Number)o).doubleValue(); try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; } }
}
