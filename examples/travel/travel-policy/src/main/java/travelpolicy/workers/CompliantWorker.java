package travelpolicy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CompliantWorker implements Worker {
    @Override public String getTaskDefName() { return "tpl_compliant"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [compliant] " + task.getInputData().get("bookingType") + " within policy — auto-approved");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("autoApproved", true); return r;
    }
}
