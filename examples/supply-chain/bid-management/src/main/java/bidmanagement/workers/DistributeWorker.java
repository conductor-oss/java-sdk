package bidmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class DistributeWorker implements Worker {
    @Override public String getTaskDefName() { return "bid_distribute"; }
    @Override public TaskResult execute(Task task) {
        List<?> vendors = (List<?>) task.getInputData().get("vendors");
        int count = vendors != null ? vendors.size() : 0;
        System.out.println("  [distribute] Sent " + task.getInputData().get("bidId") + " to " + count + " vendors");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("invitedCount", count); return r;
    }
}
