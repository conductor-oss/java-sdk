package insuranceclaims.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class PayClaimWorker implements Worker {
    @Override public String getTaskDefName() { return "clm_pay"; }
    @Override public TaskResult execute(Task task) {
        double approved = 0; Object a = task.getInputData().get("approvedAmount"); if (a instanceof Number) approved = ((Number)a).doubleValue();
        System.out.printf("  [pay] Processing payment of $%.2f to provider %s%n", approved, task.getInputData().get("providerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("paidAmount", approved); o.put("paymentId", "PAY-INS-7605"); o.put("paidAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
