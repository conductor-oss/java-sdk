package insuranceunderwriting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
public class BindWorker implements Worker {
    @Override public String getTaskDefName() { return "uw_bind"; }
    @Override public TaskResult execute(Task task) {
        String decision = (String) task.getInputData().get("decision");
        if ("accept".equals(decision)) {
            String policyNumber = "POL-" + String.valueOf(System.currentTimeMillis()).substring(5);
            System.out.println("  [bind] Policy " + policyNumber + " bound for " + task.getInputData().get("applicantName"));
            TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
            r.getOutputData().put("policyNumber", policyNumber); r.getOutputData().put("bound", true);
            r.getOutputData().put("effectiveDate", Instant.now().toString()); r.getOutputData().put("premium", task.getInputData().get("premium"));
            return r;
        }
        System.out.println("  [bind] No policy bound — decision: " + decision);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("policyNumber", "N/A"); r.getOutputData().put("bound", false);
        return r;
    }
}
