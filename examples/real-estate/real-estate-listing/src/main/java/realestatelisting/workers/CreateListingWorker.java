package realestatelisting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreateListingWorker implements Worker {
    @Override public String getTaskDefName() { return "rel_create"; }
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [create] Listing at " + task.getInputData().get("address")); result.getOutputData().put("listing", java.util.Map.of("id","LST-681","address",String.valueOf(task.getInputData().get("address")),"price",task.getInputData().get("price")));
        return result;
    }
}
