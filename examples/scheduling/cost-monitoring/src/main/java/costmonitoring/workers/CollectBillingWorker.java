package costmonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectBillingWorker implements Worker {
    @Override public String getTaskDefName() { return "cos_collect_billing"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [collect] Collecting billing data for " + task.getInputData().get("accountId"));
        r.getOutputData().put("totalSpend", 12450.00);
        r.getOutputData().put("breakdown", Map.of("compute",5500,"storage",2800,"network",1450));
        return r;
    }
}
