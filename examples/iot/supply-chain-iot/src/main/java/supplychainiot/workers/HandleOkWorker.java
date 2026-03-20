package supplychainiot.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class HandleOkWorker implements Worker {
    @Override public String getTaskDefName() { return "sci_handle_ok"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [ok] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "continue");
        r.getOutputData().put("logged", true);
        return r;
    }
}
