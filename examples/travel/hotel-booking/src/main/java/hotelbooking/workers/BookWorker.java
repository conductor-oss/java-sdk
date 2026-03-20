package hotelbooking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class BookWorker implements Worker {
    @Override public String getTaskDefName() { return "htl_book"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [book] Hotel reserved for " + task.getInputData().get("travelerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reservationId", "HTL-RES-541"); r.getOutputData().put("confirmationCode", "MRRTT-541");
        return r;
    }
}
