package insuranceclaims.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class CloseClaimWorker implements Worker {
    @Override public String getTaskDefName() { return "clm_close"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [close] Claim " + task.getInputData().get("claimId") + " closed - paid $" + task.getInputData().get("paidAmount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("claimStatus", "closed"); o.put("closedAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
