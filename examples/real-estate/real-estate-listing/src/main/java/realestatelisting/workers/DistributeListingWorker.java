package realestatelisting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DistributeListingWorker implements Worker {
    @Override public String getTaskDefName() { return "rel_distribute"; }
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [distribute] Sent to Zillow, Redfin, Realtor.com"); result.getOutputData().put("channels", java.util.List.of("Zillow","Redfin","Realtor.com")); result.getOutputData().put("distributed", true);
        return result;
    }
}
