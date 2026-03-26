package insuranceclaims.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class AdjudicateClaimWorker implements Worker {
    @Override public String getTaskDefName() { return "clm_adjudicate"; }
    @Override public TaskResult execute(Task task) {
        double amount = 0; Object a = task.getInputData().get("amount"); if (a instanceof Number) amount = ((Number)a).doubleValue();
        int coverage = 80; Object c = task.getInputData().get("coverage"); if (c instanceof Number) coverage = ((Number)c).intValue();
        double approved = Math.round(amount * coverage / 100.0 * 100.0) / 100.0;
        System.out.printf("  [adjudicate] Amount: $%.2f, Coverage: %d%%, Approved: $%.2f%n", amount, coverage, approved);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("approvedAmount", approved); o.put("patientResponsibility", amount - approved); o.put("decision", "approved");
        r.setOutputData(o); return r;
    }
}
