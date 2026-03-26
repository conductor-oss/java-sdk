package anticheat.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SuspectWorker implements Worker {
    @Override public String getTaskDefName() { return "ach_suspect"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [suspect] Player " + task.getInputData().get("playerId") + " flagged for review");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("result", "flagged_for_review");
        return r;
    }
}
