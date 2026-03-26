package coldchain.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ActWorker implements Worker {
    @Override public String getTaskDefName() { return "cch_act"; }
    @Override public TaskResult execute(Task task) {
        String action = "ok".equals(task.getInputData().get("status")) ? "logged_normal" : "corrective_action";
        System.out.println("  [act] " + task.getInputData().get("shipmentId") + ": " + action);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", action); r.getOutputData().put("logged", true); return r;
    }
}
