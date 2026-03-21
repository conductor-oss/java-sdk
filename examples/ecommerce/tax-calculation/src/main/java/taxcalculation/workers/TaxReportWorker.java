package taxcalculation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class TaxReportWorker implements Worker {
    @Override public String getTaskDefName() { return "tax_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Tax report for order " + task.getInputData().get("orderId") + ": $" + task.getInputData().get("taxAmount") + " collected");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("reported", true); o.put("reportId", "TAXRPT-" + task.getInputData().get("orderId"));
        r.setOutputData(o); return r;
    }
}
