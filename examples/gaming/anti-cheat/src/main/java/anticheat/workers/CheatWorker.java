package anticheat.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CheatWorker implements Worker {
    @Override public String getTaskDefName() { return "ach_cheat"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cheat] Player " + task.getInputData().get("playerId") + " confirmed cheating - ban issued");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("result", "banned");
        return r;
    }
}
