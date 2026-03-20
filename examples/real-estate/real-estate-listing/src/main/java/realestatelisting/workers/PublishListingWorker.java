package realestatelisting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PublishListingWorker implements Worker {
    @Override public String getTaskDefName() { return "rel_publish"; }
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [publish] Listing published to MLS"); result.getOutputData().put("listingId", "MLS-2024-681"); result.getOutputData().put("published", true);
        return result;
    }
}
