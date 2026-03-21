package anticheat.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CleanWorker implements Worker {
    @Override public String getTaskDefName() { return "ach_clean"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [clean] Player " + task.getInputData().get("playerId") + " is clean");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("result", "no_action");
        return r;
    }
}
