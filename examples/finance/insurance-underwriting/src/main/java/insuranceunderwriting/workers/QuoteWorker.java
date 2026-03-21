package insuranceunderwriting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class QuoteWorker implements Worker {
    @Override public String getTaskDefName() { return "uw_quote"; }
    @Override public TaskResult execute(Task task) {
        Object amtObj = task.getInputData().get("coverageAmount");
        double amount = amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0;
        String riskClass = (String) task.getInputData().get("riskClass");
        double riskMultiplier = "preferred".equals(riskClass) ? 0.8 : "substandard".equals(riskClass) ? 1.5 : 1.0;
        double basePremium = (amount / 1000) * 1.5;
        double premium = Math.round(basePremium * riskMultiplier * 100.0) / 100.0;
        System.out.println("  [quote] Coverage: $" + amount + ", Risk: " + riskClass + ", Premium: $" + premium + "/month");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("premium", premium); r.getOutputData().put("premiumFrequency", "monthly");
        r.getOutputData().put("quoteValidDays", 30);
        return r;
    }
}
