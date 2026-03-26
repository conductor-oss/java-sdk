package flashsale.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class OpenSaleWorker implements Worker {
    @Override public String getTaskDefName() { return "fls_open_sale"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [open] Flash sale " + task.getInputData().get("saleId") + " is LIVE for " + task.getInputData().get("durationMinutes") + " minutes");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("status", "live"); o.put("openedAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
