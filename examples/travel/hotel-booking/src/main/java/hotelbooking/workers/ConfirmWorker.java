package hotelbooking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "htl_confirm"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Reservation " + task.getInputData().get("reservationId") + " confirmed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", true); return r;
    }
}
