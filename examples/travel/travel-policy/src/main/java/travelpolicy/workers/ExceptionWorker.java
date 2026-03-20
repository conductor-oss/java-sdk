package travelpolicy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ExceptionWorker implements Worker {
    @Override public String getTaskDefName() { return "tpl_exception"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [exception] " + task.getInputData().get("bookingType") + " exception: " + task.getInputData().get("reason"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("exceptionApproved", true); r.getOutputData().put("approvedBy", "VP-Travel"); return r;
    }
}
