package carrental.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class BookWorker implements Worker {
    @Override public String getTaskDefName() { return "crl_book"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [book] Vehicle reserved for " + task.getInputData().get("travelerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reservationId", "CRL-542"); r.getOutputData().put("confirmationCode", "HERTZ-542");
        return r;
    }
}
