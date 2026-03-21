package donormanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RetainWorker implements Worker {
    @Override public String getTaskDefName() { return "dnr_retain"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [retain] Retention plan for donor " + task.getInputData().get("donorId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("level", "mid-level"); r.addOutputData("retained", true); r.addOutputData("nextAction", "annual-report"); return r;
    }
}
