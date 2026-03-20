package carrental.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ReturnWorker implements Worker {
    @Override public String getTaskDefName() { return "crl_return"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [return] Vehicle returned — " + task.getInputData().get("reservationId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("returned", true); r.getOutputData().put("totalCost", 195); r.getOutputData().put("mileageEnd", 24850);
        return r;
    }
}
