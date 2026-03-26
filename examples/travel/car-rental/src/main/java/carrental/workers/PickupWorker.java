package carrental.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class PickupWorker implements Worker {
    @Override public String getTaskDefName() { return "crl_pickup"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [pickup] Vehicle picked up — reservation " + task.getInputData().get("reservationId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("pickedUp", true); r.getOutputData().put("mileageStart", 24500);
        return r;
    }
}
