package insuranceclaims.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class VerifyClaimWorker implements Worker {
    @Override public String getTaskDefName() { return "clm_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Checking eligibility for patient " + task.getInputData().get("patientId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("eligible", true); o.put("coverage", 80); o.put("deductibleMet", true); o.put("planType", "PPO");
        r.setOutputData(o); return r;
    }
}
