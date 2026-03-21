package insuranceclaims.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;
public class SubmitClaimWorker implements Worker {
    @Override public String getTaskDefName() { return "clm_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Claim " + task.getInputData().get("claimId") + ": $" + task.getInputData().get("amount") + " for " + task.getInputData().get("procedureCode"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("submitted", true); o.put("submittedAt", Instant.now().toString()); o.put("referenceNo", "REF-CLM-5501");
        r.setOutputData(o); return r;
    }
}
