package playerprogression.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CompleteTaskWorker implements Worker {
    @Override public String getTaskDefName() { return "ppg_complete_task"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [complete] Player " + task.getInputData().get("playerId") + " completed quest " + task.getInputData().get("questId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("completed", true); r.addOutputData("questName", "Dragon's Lair");
        return r;
    }
}
