package cryptocurrencytrading.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ExecuteSellWorker implements Worker {
    @Override public String getTaskDefName() { return "cry_execute_sell"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [sell] Selling " + task.getInputData().get("amount") + " " + task.getInputData().get("pair") + " at $" + task.getInputData().get("price"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("orderId", "ORD-510-SELL-001"); r.getOutputData().put("filledPrice", 67435.00);
        r.getOutputData().put("filledAmount", 0.15); r.getOutputData().put("fee", 10.12);
        return r;
    }
}
