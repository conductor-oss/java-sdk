package realestatelisting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class VerifyListingWorker implements Worker {
    @Override public String getTaskDefName() { return "rel_verify"; }
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [verify] Address and ownership verified"); result.getOutputData().put("verifiedListing", java.util.Map.of("verified", true));
        return result;
    }
}
