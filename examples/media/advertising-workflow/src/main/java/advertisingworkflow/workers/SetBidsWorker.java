package advertisingworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SetBidsWorker implements Worker {
    @Override public String getTaskDefName() { return "adv_set_bids"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [bid] Processing " + task.getInputData().getOrDefault("bidStrategy", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("bidStrategy", "target_cpa");
        r.getOutputData().put("dailyBudget", "Math.round(budget / 30)");
        r.getOutputData().put("maxBid", "cpmBid * 1.5");
        return r;
    }
}
