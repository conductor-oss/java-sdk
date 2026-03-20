package flashsale.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "fls_report"; }
    @Override public TaskResult execute(Task task) {
        int filled = 0; Object f = task.getInputData().get("ordersFilled"); if (f instanceof Number) filled = ((Number)f).intValue();
        int remaining = 0; Object rm = task.getInputData().get("remainingStock"); if (rm instanceof Number) remaining = ((Number)rm).intValue();
        int total = filled + remaining;
        int soldOutPercent = total > 0 ? Math.round((float) filled / total * 100) : 0;
        System.out.println("  [report] " + task.getInputData().get("saleName") + ": " + soldOutPercent + "% sold, $" + task.getInputData().get("revenue") + " revenue");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("soldOutPercent", soldOutPercent); o.put("reportGenerated", true);
        r.setOutputData(o); return r;
    }
}
