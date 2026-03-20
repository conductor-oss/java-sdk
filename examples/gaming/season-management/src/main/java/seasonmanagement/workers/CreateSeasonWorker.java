package seasonmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreateSeasonWorker implements Worker {
    @Override public String getTaskDefName() { return "smg_create_season"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [create] Creating Season " + task.getInputData().get("seasonNumber") + ": " + task.getInputData().get("theme"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("seasonId", "S3-2026"); r.addOutputData("created", true);
        return r;
    }
}
