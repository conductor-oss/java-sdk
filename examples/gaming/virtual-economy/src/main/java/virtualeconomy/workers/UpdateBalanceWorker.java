package virtualeconomy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class UpdateBalanceWorker implements Worker {
    @Override public String getTaskDefName() { return "vec_update_balance"; }
    @Override public TaskResult execute(Task task) {
        int amount = 100;
        Object a = task.getInputData().get("amount");
        if (a instanceof Number) amount = ((Number) a).intValue();
        System.out.println("  [balance] Updating balance: +" + amount);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("newBalance", 5200 + amount); r.addOutputData("previousBalance", 5200);
        return r;
    }
}
