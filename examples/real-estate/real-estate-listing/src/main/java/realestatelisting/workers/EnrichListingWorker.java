package realestatelisting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class EnrichListingWorker implements Worker {
    @Override public String getTaskDefName() { return "rel_enrich"; }
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [enrich] Added comps, school district, walkability score"); result.getOutputData().put("enrichedListing", java.util.Map.of("walkScore",85,"schoolRating",8));
        return result;
    }
}
