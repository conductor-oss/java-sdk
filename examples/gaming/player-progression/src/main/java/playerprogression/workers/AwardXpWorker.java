package playerprogression.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AwardXpWorker implements Worker {
    @Override public String getTaskDefName() { return "ppg_award_xp"; }
    @Override public TaskResult execute(Task task) {
        int xp = 500;
        Object xe = task.getInputData().get("xpEarned");
        if (xe instanceof Number) xp = ((Number) xe).intValue();
        System.out.println("  [xp] Awarding " + xp + " XP to " + task.getInputData().get("playerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("totalXp", 4800 + xp); r.addOutputData("awarded", xp);
        return r;
    }
}
