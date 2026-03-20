package investmentworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "ivt_execute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [execute] " + task.getInputData().get("action") + " " + task.getInputData().get("shares") + " shares of " + task.getInputData().get("tickerSymbol"));
        Object sharesObj = task.getInputData().get("shares");
        int shares = sharesObj instanceof Number ? ((Number)sharesObj).intValue() : 0;
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("tradeId", "TRD-509-001"); r.getOutputData().put("executedPrice", 185.25);
        r.getOutputData().put("totalCost", shares * 185.25);
        return r;
    }
}
