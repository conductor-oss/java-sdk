package benefitsenrollment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "ben_confirm"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Confirmation sent for enrollment " + task.getInputData().get("enrollmentId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", true);
        r.getOutputData().put("cardsMailed", true);
        return r;
    }
}
