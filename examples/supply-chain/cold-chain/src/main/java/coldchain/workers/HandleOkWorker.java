package coldchain.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class HandleOkWorker implements Worker {
    @Override public String getTaskDefName() { return "cch_handle_ok"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [ok] " + task.getInputData().get("shipmentId") + ": temperature within range — no action needed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "continue_monitoring"); return r;
    }
}
