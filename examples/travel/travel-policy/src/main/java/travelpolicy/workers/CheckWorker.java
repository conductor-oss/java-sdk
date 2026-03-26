package travelpolicy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CheckWorker implements Worker {
    @Override public String getTaskDefName() { return "tpl_check"; }
    @Override public TaskResult execute(Task task) {
        int amount = Integer.parseInt(String.valueOf(task.getInputData().get("amount")));
        String tier = String.valueOf(task.getInputData().get("policyTier"));
        int limit = "executive".equals(tier) ? 500 : 250;
        String result = amount <= limit ? "compliant" : "exception";
        System.out.println("  [check] " + task.getInputData().get("bookingType") + ": $" + amount + " vs $" + limit + " — " + result);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("complianceResult", result);
        r.getOutputData().put("reason", "exception".equals(result) ? "exceeds-limit" : null);
        return r;
    }
}
