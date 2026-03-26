package interviewscheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "ivs_confirm"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Interview " + task.getInputData().get("interviewId") + " confirmed by all parties");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", true);
        r.getOutputData().put("confirmations", 4);
        return r;
    }
}
