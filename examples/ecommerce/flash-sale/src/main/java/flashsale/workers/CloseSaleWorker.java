package flashsale.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class CloseSaleWorker implements Worker {
    @Override public String getTaskDefName() { return "fls_close_sale"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [close] Flash sale closed - " + task.getInputData().get("ordersFilled") + " orders filled");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("status", "closed"); o.put("closedAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
