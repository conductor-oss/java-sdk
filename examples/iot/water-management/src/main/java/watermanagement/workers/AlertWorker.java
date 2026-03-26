package watermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AlertWorker implements Worker {
    @Override public String getTaskDefName() { return "wtr_alert"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [alert] Processing task");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("done", true);
        return r;
    }
}
